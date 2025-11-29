package com.minecraftcivilizations.specialization.Listener.Player;

import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Specialization;
import com.minecraftcivilizations.specialization.StaffTools.Debug;
import com.mojang.datafixers.DataFixerBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.*;

import java.lang.Math;
import java.util.*;
import java.util.logging.Logger;

public class LocalChat implements Listener {

    // ---- CONSTANTS ----
    private static final int MAX_CHARS = 155;
    private static final int MAX_BUBBLES = 3;
    private static final float BASE_HEIGHT = 0.6f;
    private static final float NAMEPLATE_OFFSET = -0.4f;
    private static final float SINGLE_LINE_SPACING = 0.3f;
    private static final float MULTILINE_SPACING = 0.3f;
    private static final float NEW_BUBBLE_OFFSET = -0.22f;
    private static final float LERP_RATE = 0.26f;
    private static final float BOB_AMPLITUDE = 0.02f;
    private static final double BOB_PERIOD_MS = 3000.0;
    private static final long LIFETIME_TICKS = 200L;
    private static final int CHARS_PER_TICK = 3;
    private static final int FADE_TICKS = 20;
    private static final String CHAT_COLOR = "<#f5f2c8>";
    private static final String QUOTE_COLOR = "<#b7a96f>";
    private static final boolean POP_SOUND = true;
    private static final float POP_VOLUME = 0.6f;
    private static final float POP_PITCH_VARIANCE = 0.2f;

    // toggle typing animation on/off
    private static final boolean ENABLE_TYPING_ANIMATION = true;

    private static final int BOB_PERIOD_TICKS = Math.max(1, (int) (BOB_PERIOD_MS / 50.0));
    private static final float[] BOB_TABLE = new float[BOB_PERIOD_TICKS];
    static {
        for (int i = 0; i < BOB_PERIOD_TICKS; i++) {
            double phase = (i / (double) BOB_PERIOD_TICKS) * Math.PI * 2.0;
            BOB_TABLE[i] = (float) (Math.sin(phase) * BOB_AMPLITUDE);
        }
    }

    // ---- DATA CLASSES ----
    private static class BubbleData {
        final TextDisplay td;
        final Component[] frames;
        final int msgLength;
        float currentY, targetY;
        int life;
        int frameIndex = 0;
        float opacity = 1f;
        Transformation transform;
        // scale animation (squash & stretch)
        Vector3f currentScale = new Vector3f(0.6f, 1.4f, 0.6f);
        final Vector3f targetScale = new Vector3f(1f, 1f, 1f);

        BubbleData(TextDisplay td, Component[] frames, int msgLength, float startY, float targetY) {
            this.td = td;
            this.frames = frames;
            this.msgLength = msgLength;
            this.currentY = startY;
            this.targetY = targetY;
            this.life = (int) LIFETIME_TICKS;
            this.transform = td.getTransformation();
        }
    }

    private static class PlayerSession {
        final Player player;
        final List<BubbleData> bubbles = new ArrayList<>();
        ArmorStand nameplate;
        PlayerSession(Player p){ this.player = p; }
    }

    private final Map<UUID, PlayerSession> sessions = new HashMap<>();
    private boolean running = false;

    // ---- EVENTS ----
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        String raw = MiniMessage.miniMessage().stripTags(e.getMessage().trim());

        if (handleGlobalChat(p, raw)) {
            e.setCancelled(true);
            return;
        }

        // Always spawn bubble (UI) for everyone — schedule to main thread
        Bukkit.getScheduler().runTask(Specialization.getInstance(), () -> spawnBubble(p, raw));
        Debug.broadcast("globalchat", "<gray>"+p.getName()+" » </gray>" + e.getMessage());
        if (p.getGameMode() == GameMode.SPECTATOR || p.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
            e.setCancelled(true);
            return;
        }

        // For normal players: cancel default and do proximity broadcast
        e.setCancelled(true);
        String fmt = SpecializationConfig.getChatConfig().get("DEFAULT_FORMAT", String.class);
        Bukkit.getScheduler().runTask(Specialization.getInstance(), () -> {
            for (Player near : getNearbyPlayers(p))
                near.sendRichMessage(fmt.formatted(p.getName(), raw));
            p.sendRichMessage(fmt.formatted(p.getName(), raw));
        });
    }

    // ---- SPAWNING ----
    public void spawnBubble(Player p, String msg) {

        msg = truncate(msg);
        int msgLen = msg.length();

        int lines = getLineCount(msg);
        float bubbleHeight = lines > 1 ? lines * MULTILINE_SPACING : SINGLE_LINE_SPACING;

        PlayerSession s = sessions.computeIfAbsent(p.getUniqueId(), k -> {
            PlayerSession ps = new PlayerSession(p);
            // Only spawn nameplate if player is visible and not spectator
            if (p.getGameMode() != GameMode.SPECTATOR && !p.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                spawnNameplate(p, ps);
            }
            return ps;
        });

        // compute total chars (use msgLength stored)
        int totalChars = msgLen + s.bubbles.stream().mapToInt(b -> b.msgLength).sum();

        // --- Context-based deletion thresholds ---
        if (totalChars > 250) {
            int toRemove = Math.min(2, s.bubbles.size());
            for (int i = 0; i < toRemove; i++) {
                BubbleData removed = s.bubbles.removeFirst();
                removeBubble(removed);
            }
        } else if (totalChars > 150) {
            if (!s.bubbles.isEmpty()) {
                BubbleData removed = s.bubbles.removeFirst();
                removeBubble(removed);
            }
        }

        // shift remaining bubbles upward
        s.bubbles.forEach(b -> b.targetY += bubbleHeight);

        // spawn text display at world position (bubbles follow as passenger so coordinates relative to mount are okay)
        Location spawn = p.getEyeLocation().clone();
        spawn.setY(spawn.getY() + BASE_HEIGHT + NEW_BUBBLE_OFFSET - 0.5);

        TextDisplay td = p.getWorld().spawn(spawn, TextDisplay.class, t -> {
            t.text(MiniMessage.miniMessage().deserialize(CHAT_COLOR));
            t.setDefaultBackground(false);
            t.setBackgroundColor(Color.fromARGB(1, 0, 0, 0));
            t.setBillboard(Display.Billboard.CENTER);
            t.setShadowed(true);
            t.setSeeThrough(false);
            t.setViewRange(32f);
            t.setPersistent(false);
            t.setInterpolationDuration(0);
            // initial squash & stretch scale
            t.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0.2f),
                    new AxisAngle4f(),
                    new Vector3f(0.6f, 1.4f, 0.6f),
                    new AxisAngle4f()
            ));
        });

        Component[] frames = prebuildFrames(msg);
        BubbleData data = new BubbleData(td, frames, msgLen, BASE_HEIGHT + NEW_BUBBLE_OFFSET, BASE_HEIGHT);

        // If typing animation disabled, set final frame immediately
        if (!ENABLE_TYPING_ANIMATION && frames.length > 0) {
            data.frameIndex = frames.length - 1;
            td.text(frames[data.frameIndex]);
        }

        s.bubbles.add(data);
        // attach to player as passenger so it follows them
        p.addPassenger(td);
        playPop(p);
        startScheduler();
    }

    private void spawnNameplate(Player p, PlayerSession s) {
        Location eye = p.getEyeLocation().clone();
        eye.setY(eye.getY() + BASE_HEIGHT + NAMEPLATE_OFFSET);
        ArmorStand as = p.getWorld().spawn(eye, ArmorStand.class, a -> {
            a.setCustomName(p.getName());
            a.setCustomNameVisible(true);
            a.setInvisible(true);
            a.setMarker(true);
            a.setGravity(false);
            a.setSmall(true);
            a.setInvulnerable(true);
            a.setPersistent(false);
        });
        s.nameplate = as;
        p.addPassenger(as);
        //hides from the owner
         p.hideEntity(Specialization.getInstance(), as);
    }

    // ---- MAIN LOOP ----
    private void startScheduler() {
        if (running) return;
        running = true;

        new BukkitRunnable() {
            int bobIndex = 0;
            @Override public void run() {

                bobIndex = (bobIndex + 1) % BOB_PERIOD_TICKS;
                float bob = BOB_TABLE[bobIndex];
                boolean anyAlive = false;

                for (Iterator<PlayerSession> it = sessions.values().iterator(); it.hasNext();) {
                    PlayerSession s = it.next();

                    // remove session if player logged out / offline
                    if (s.player == null || !s.player.isOnline()) {
                        // cleanup everything for this session
                        s.bubbles.forEach(LocalChat.this::removeBubble);
                        if (s.nameplate != null) s.nameplate.remove();
                        it.remove();
                        continue;
                    }

                    for (Iterator<BubbleData> bit = s.bubbles.iterator(); bit.hasNext();) {
                        BubbleData b = bit.next();
                        TextDisplay td = b.td;

                        // typing animation: advance frames if enabled
                        if (ENABLE_TYPING_ANIMATION && b.frameIndex < b.frames.length) {
                            td.text(b.frames[b.frameIndex++]);
                        }

                        // position lerp
                        b.currentY += (b.targetY - b.currentY) * LERP_RATE;

                        // update transform translation (reuse transform object for rotations)
                        Vector3f translation = b.transform.getTranslation();
                        translation.y = b.currentY + bob;

                        // scale lerp (squash & stretch easing)
                        Vector3f scale = b.currentScale;
                        scale.x += (b.targetScale.x - scale.x) * 0.2f;
                        scale.y += (b.targetScale.y - scale.y) * 0.5f;
                        scale.z += (b.targetScale.z - scale.z) * 0.2f;

                        // build new transformation with updated scale and translation but keep rotations
                        Transformation newT = new Transformation(
                                translation,
                                b.transform.getLeftRotation(),
                                scale,
                                b.transform.getRightRotation()
                        );

                        b.transform = newT;
                        td.setTransformation(newT);

                        // fade near end of life
                        if (b.life-- < FADE_TICKS) {
                            b.opacity -= 1f / FADE_TICKS;
                            td.setTextOpacity((byte) (Math.max(0f, Math.min(1f, b.opacity)) * 255));
                        }

                        if (b.life <= 0) {
                            removeBubble(b);
                            bit.remove();
                        } else {
                            anyAlive = true;
                        }
                    }

                    // if no bubbles left, clean up nameplate/session
                    if (s.bubbles.isEmpty()) {
                        if (s.nameplate != null) s.nameplate.remove();
                        it.remove();
                    }
                }

                if (!anyAlive) { running = false; cancel(); }
            }
        }.runTaskTimer(Specialization.getInstance(), 0L, 1L);
    }

    // ---- HELPERS ----
    private Component[] prebuildFrames(String msg) {
        MiniMessage mm = MiniMessage.miniMessage();
        int len = msg.length();
        int steps = Math.max(1, (int) Math.ceil(len / (float) CHARS_PER_TICK));
        Component[] frames = new Component[steps];
        for (int i = 0; i < steps; i++) {
            int end = Math.min(len, (i + 1) * CHARS_PER_TICK);
            String sub = msg.substring(0, end);
            frames[i] = mm.deserialize(QUOTE_COLOR + "“" + CHAT_COLOR + sub + QUOTE_COLOR + "”");
        }
        return frames;
    }

    // simple approximation used before — keeps original behaviour
    int getLineCount(String message) {
        int explicit = message.split("\n", -1).length;
        int approx = (int) Math.ceil(message.length() / 30.0);
        return Math.max(explicit, approx);
    }

    private void removeBubble(BubbleData b){
        if (b == null) return;
        if (!b.td.isDead()) b.td.remove();
    }

    private void removeAll(Player p){
        PlayerSession s = sessions.remove(p.getUniqueId());
        if(s == null) return;
        s.bubbles.forEach(this::removeBubble);
        if(s.nameplate != null) s.nameplate.remove();
    }

    private String truncate(String m){ return m.length() <= MAX_CHARS ? m : m.substring(0, MAX_CHARS - 3) + "..."; }

    private void playPop(Player p){
    float pitch = 0.8f - (POP_PITCH_VARIANCE / 2f) + (float) (Math.random() * POP_PITCH_VARIANCE);

        if(!POP_SOUND) return;
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PUFFER_FISH_BLOW_UP, SoundCategory.UI, POP_VOLUME, pitch);

    }

    private boolean handleGlobalChat(Player p, String msg){
        String prefix = SpecializationConfig.getChatConfig().get("ANNOUNCEMENT_PREFIX", String.class);
        if(!msg.startsWith(prefix) || !p.isOp()) return false;
        String actual = msg.substring(prefix.length()).trim();
        String fmt = SpecializationConfig.getChatConfig().get("ANNOUNCEMENT_FORMAT", String.class);
        Bukkit.getOnlinePlayers().forEach(pl -> pl.sendRichMessage(fmt.formatted(actual)));
        return true;
    }

    private List<Player> getNearbyPlayers(Player p){
        double r = SpecializationConfig.getChatConfig().get("CHAT_RADIUS", Double.class);
        return p.getNearbyEntities(r,r,r).stream().filter(e -> e instanceof Player).map(e -> (Player)e).toList();
    }

    @EventHandler public void onDeath(PlayerDeathEvent e) { removeAll(e.getEntity()); }
    @EventHandler public void onQuit(PlayerQuitEvent e) { e.quitMessage(null); removeAll(e.getPlayer()); }
    @EventHandler public void onJoin(PlayerJoinEvent e) { e.joinMessage(null); }
    @EventHandler public void onWorldChange(PlayerChangedWorldEvent e) { removeAll(e.getPlayer()); }
}
