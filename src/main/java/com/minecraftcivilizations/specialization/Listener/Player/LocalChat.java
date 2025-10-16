package com.minecraftcivilizations.specialization.Listener.Player;

import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Specialization;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.*;

/**
 * LocalChat - chat bubble system with smooth movement, bobbing and pop sound.
 */
public class LocalChat implements Listener {
    //max characters in a bubble msg
    private static final int maxChars = 155;
    //typing animation grouping
    private static final int charsPerTick = 3; // more chars = more performant at scale
    private static final long animationTickSpeed = 1L;
    // Base height above head
    private static final float BASE_HEIGHT = 0.6f;
    private static final float NAMEPLATE_HEIGHT_OFFSET = -0.4f; //positive is up negative is down. Spawn location of nameplate


    // Default vertical spacing between messages (for 1-line msgs)
    private static final float SINGLE_LINE_SPACING = 0.30f;

    // Additional spacing per line when message is multi-line
    private static final float MULTILINE_SPACING = 0.30f;

    // Color wrap strings used with MiniMessage
    private static final String QOUTE_COLOR = "<#b7a96f>"; // slightly darker yellow than chat bubble
    private static final String CHAT_BUBBLE_COLOR = "<#f5f2c8>"; // main chat text

    // thresholds
    private static final int MAX_BUBBLES = 3;
    private static final long LIFETIME_TICKS = 20L * 10;

    // LERP / bobbing settings
    private static final float LERP_RATE = 0.26f;        // how quickly current Y approaches target Y
    private static final float NEW_BUBBLE_OFFSET = -0.22f; // start a bit lower and rise in
    private static final float BOB_AMPLITUDE = 0.02f;   // bob amplitude
    private static final double BOB_PERIOD_MS = 3000.0; // one bob period in ms

    // pop sound radius
    private static final double POP_SOUND_RADIUS = 10.0;
    private static final boolean POP_SOUND_ENABLED = true;
    private static final boolean POP_PLAY_FOR_SENDER = true;
    private static final float POP_VOLUME = 0.6f;
    private static final float POP_PITCH_VARIANCE = 0.2f;

    private final Map<UUID, List<TextDisplay>> activeBubbles = new HashMap<>();
    private final Map<UUID, ArmorStand> activeNames = new HashMap<>();

    // track original (non-bobbing) target Y for each bubble so stacking adjustments are stable
    private final Map<TextDisplay, Float> targetY = new HashMap<>();
    // track current Y used by animation loop (keeps persisted state across ticks)
    private final Map<TextDisplay, Float> currentY = new HashMap<>();
    // store messages for char-based thresholds
    private final Map<TextDisplay, String> bubbleMessages = new HashMap<>();

    private boolean animatorRunning = false;

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        String message = MiniMessage.miniMessage().stripTags(event.getMessage().trim());

        // Handle announcements first
        if (tryHandleGlobalChat(sender, message)) {
            event.setCancelled(true); // announcements shouldn't appear in normal chat
            return;
        }

        // Always show the bubble
        Bukkit.getScheduler().runTask(Specialization.getInstance(), () -> showChatBubble(sender, message));

        // Cancel vanilla chat only if the sender is spectator or invisible
        if (sender.getGameMode() == GameMode.SPECTATOR || sender.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
            event.setCancelled(true);
            return;
        }

        // Vanilla chat: send only to nearby players and the sender
        event.setCancelled(true); // cancel default broadcast
        String format = SpecializationConfig.getChatConfig().get("DEFAULT_FORMAT", String.class);
        Bukkit.getScheduler().runTask(Specialization.getInstance(), () -> {
            for (Player near : getNearbyPlayers(sender))
                near.sendRichMessage(format.formatted(sender.getName(), message));
            sender.sendRichMessage(format.formatted(sender.getName(), message));
        });
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        removeAllBubbles(player);
    }


    private void showChatBubble(Player player, String message) {
        message = truncateMessage(message);
        int lineCount = getLineCount(message);

        float bubbleHeight = lineCount > 1 ? lineCount * MULTILINE_SPACING : SINGLE_LINE_SPACING;

        UUID uuid = player.getUniqueId();
        List<TextDisplay> bubbles = activeBubbles.computeIfAbsent(uuid, k -> new ArrayList<>());

        if (!activeNames.containsKey(uuid)) spawnNameplate(player);

        // --- Calculate total characters including all bubbles ---
        int totalChars = message.length();
        for (TextDisplay td : bubbles) {
            if (!td.isDead() && bubbleMessages.containsKey(td)) {
                totalChars += bubbleMessages.get(td).length();
            }
        }

        // --- Immediate deletion thresholds ---
        if (totalChars > 250) { // huge total, remove all except newest
            for (TextDisplay td : new ArrayList<>(bubbles)) {
                removeBubble(td, player, true);
            }
            bubbles.clear();
        } else if (totalChars > maxChars) { // remove only oldest
            if (!bubbles.isEmpty()) {
                TextDisplay oldest = bubbles.removeFirst();
                removeBubble(oldest, player, true);
            }
        }

        // --- Enforce MAX_BUBBLES ---
        while (bubbles.size() >= MAX_BUBBLES) {
            TextDisplay oldest = bubbles.removeFirst();
            removeBubble(oldest, player, true);
        }

        // --- Shift remaining bubbles upward by increasing their targetY by bubbleHeight ---
        for (TextDisplay td : bubbles) {
            if (td.isDead()) continue;
            float prevTarget = targetY.getOrDefault(td, BASE_HEIGHT);
            float newTarget = prevTarget + bubbleHeight;
            targetY.put(td, newTarget);
        }

        // compute world spawn location that compensates for mount pivot (put it above the head)
        org.bukkit.Location spawnLoc = player.getLocation().clone();
        double headY = player.getEyeLocation().getY(); // absolute world y of eyes
        double desiredWorldY = headY + BASE_HEIGHT + NEW_BUBBLE_OFFSET + -0.5; // desired world Y above head
        spawnLoc.setY(desiredWorldY);

        // spawn the text display at that world position (so it starts visually above the head)
        TextDisplay td = player.getWorld().spawn(spawnLoc, TextDisplay.class, spawned -> {
            // Start with empty text but colored properly (apply color markup later in animateText)
            spawned.text(MiniMessage.miniMessage().deserialize(CHAT_BUBBLE_COLOR + ""));

            spawned.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            spawned.setDefaultBackground(false);
            // Settings
            spawned.setBillboard(Display.Billboard.CENTER);
            spawned.setShadowed(true);
            spawned.setSeeThrough(false);
            spawned.setViewRange(32f);
            spawned.setPersistent(false);
            spawned.setInterpolationDuration(0); // no interpolation - place instantly
            spawned.setBrightness(new Display.Brightness(10, 10));


            Vector3f translation = new Vector3f(0f, 0f, 0.2f);
            Vector3f scale = new Vector3f(1f, 1f, 1f);
            AxisAngle4f rotation = new AxisAngle4f(0, 0, 1, 0);
            spawned.setTransformation(new Transformation(translation, rotation, scale, rotation));
        });

        // initialize animation state: currentY/targetY are still relative to the player's head baseline
        currentY.put(td, (float) (BASE_HEIGHT + NEW_BUBBLE_OFFSET));
        targetY.put(td, BASE_HEIGHT);
        bubbleMessages.put(td, message);
        animateText(td, message);

        // attach to player so it follows as a passenger
        player.addPassenger(td);
        bubbles.add(td);


        // play bubble pop sound to nearby players
        playPopSound(player, player.getLocation());

        // start animator if not running
        startAnimatorIfNeeded();

        // --- Schedule fade-out normally ---
        Bukkit.getScheduler().runTaskLater(Specialization.getInstance(), () -> {
            List<TextDisplay> list = activeBubbles.get(uuid);
            if (list != null) list.remove(td);
            bubbleMessages.remove(td);
            targetY.remove(td);
            currentY.remove(td);
            removeBubble(td, player, false);
        }, LIFETIME_TICKS);
    }



    private void playPopSound(Player sender, org.bukkit.Location location) {
        if (!POP_SOUND_ENABLED) return;

        double r2 = POP_SOUND_RADIUS * POP_SOUND_RADIUS;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getWorld().equals(location.getWorld())) continue;
            if (!POP_PLAY_FOR_SENDER && p.equals(sender)) continue; // skip sender if disabled
            if (p.getLocation().distanceSquared(location) > r2) continue;

            float pitch = 0.9f - (POP_PITCH_VARIANCE / 2f) + (float) (Math.random() * POP_PITCH_VARIANCE);
            p.playSound(location, Sound.ENTITY_PUFFER_FISH_BLOW_UP, SoundCategory.UI, POP_VOLUME, pitch);
        }
    }

    /** --- Fade + cleanup --- **/
    private void removeBubble(TextDisplay td, Player player, boolean forceImmediate) {
        bubbleMessages.remove(td); // stop tracking message

        if (td.isDead()) {
            checkRemoveNameplate(player);
            return;
        }

        if (forceImmediate) {
            // remove instantly
            targetY.remove(td);
            currentY.remove(td);
            td.remove();
            Bukkit.getScheduler().runTaskLater(Specialization.getInstance(), () -> checkRemoveNameplate(player), 1L);
            return;
        }

        // fade out normally
        new BukkitRunnable() {
            float opacity = 1f;

            @Override
            public void run() {
                if (td.isDead()) {
                    cancel();
                    checkRemoveNameplate(player);
                    return;
                }
                opacity -= 0.1f;
                td.setTextOpacity((byte) (opacity * 255));
                if (opacity <= 0) {
                    // ensure removed and cleanup maps
                    targetY.remove(td);
                    currentY.remove(td);
                    td.remove();
                    cancel();
                    Bukkit.getScheduler().runTaskLater(Specialization.getInstance(), () -> checkRemoveNameplate(player), 1L);
                }
            }
        }.runTaskTimer(Specialization.getInstance(), 0L, 1L);
    }

    private void checkRemoveNameplate(Player player) {
        UUID uuid = player.getUniqueId();
        List<TextDisplay> bubbles = activeBubbles.get(uuid);
        boolean anyAlive = bubbles != null && bubbles.stream().anyMatch(td -> !td.isDead());
        if (!anyAlive) removeNameplate(player);
    }

    private void spawnNameplate(Player sender) {
        if (sender.getGameMode() == GameMode.SPECTATOR || sender.hasPotionEffect(PotionEffectType.INVISIBILITY))
            return;

        UUID uuid = sender.getUniqueId();

        // Compute world location above the player's head
        Location eye = sender.getEyeLocation().clone();
        double spawnY = eye.getY() + BASE_HEIGHT + NAMEPLATE_HEIGHT_OFFSET; // adjust offset as needed
        eye.setY(spawnY);

        // Spawn ArmorStand as a passenger
        ArmorStand nameplate = sender.getWorld().spawn(eye, ArmorStand.class, as -> {
            as.setCustomName(sender.getName());
            as.setCustomNameVisible(true);
            as.setGravity(false); // so it doesn't fall
            as.setInvisible(true); // hide the model, only show name
            as.setMarker(true); // false = keeps collision box, true = no hitbox
            as.setPersistent(true); // prevents despawning
            as.setSmall(true); // adjust if you want a smaller stand
            as.setArms(false);
            as.setBasePlate(false);
            as.setInvulnerable(true);
        });

        activeNames.put(uuid, nameplate);

        // Add as passenger AFTER spawning
        sender.addPassenger(nameplate);

        // Optionally hide for sender if needed (like you did with TextDisplay)
//        sender.hideEntity(Specialization.getInstance(), nameplate);
    }






    private void removeNameplate(Player player) {
        UUID uuid = player.getUniqueId();
        ArmorStand nameplate = activeNames.remove(uuid);
        if (nameplate != null && !nameplate.isDead()) nameplate.remove();
    }

    /** --- Typing animation --- **/
    private void animateText(TextDisplay td, String message) {
        final char[] chars = message.toCharArray();
        final StringBuilder builder = new StringBuilder();

        new BukkitRunnable() {
            int index = 0;
            @Override
            public void run() {
                if (td.isDead() || index >= chars.length) {
                    cancel();
                    return;
                }

                // Append multiple chars per tick for speed
                for (int i = 0; i < charsPerTick && index < chars.length; i++, index++) {
                    builder.append(chars[index]);
                }

                String partial = QOUTE_COLOR + "“" + CHAT_BUBBLE_COLOR + builder + QOUTE_COLOR + "”";
                td.text(MiniMessage.miniMessage().deserialize(partial));
            }
        }.runTaskTimer(Specialization.getInstance(), 0L, animationTickSpeed); // 1 tick interval
    }


    /** --- Animator task (lerp + bob) --- **/
    private void startAnimatorIfNeeded() {
        if (animatorRunning) return;
        animatorRunning = true;

        BukkitRunnable animatorTask = new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                double bobPhase = (now % (long) BOB_PERIOD_MS) / BOB_PERIOD_MS * Math.PI * 2.0;
                float bobOffset = (float) (Math.sin(bobPhase) * BOB_AMPLITUDE);

                // iterate through all active bubbles across players
                for (Map.Entry<UUID, List<TextDisplay>> entry : activeBubbles.entrySet()) {
                    List<TextDisplay> list = entry.getValue();
                    if (list == null || list.isEmpty()) continue;

                    // For each bubble, lerp currentY toward targetY and apply bob
                    for (TextDisplay td : new ArrayList<>(list)) {
                        if (td == null || td.isDead()) {
                            // cleanup
                            targetY.remove(td);
                            currentY.remove(td);
                            bubbleMessages.remove(td);
                            continue;
                        }

                        float tgt = targetY.getOrDefault(td, BASE_HEIGHT);
                        float cur = currentY.getOrDefault(td, tgt);
                        // lerp towards target
                        float next = cur + (tgt - cur) * LERP_RATE;
                        currentY.put(td, next);

                        // apply bob on top of next
                        float displayY = next + bobOffset;

                        // update transformation (only change translation Y)
                        Transformation t = td.getTransformation();
                        Vector3f translation = new Vector3f(t.getTranslation());
                        translation.y = displayY;
                        t.getTranslation().set(translation);
                        td.setTransformation(t);
                    }
                }

                // stop animator if nothing left to animate
                boolean anyAlive = activeBubbles.values().stream().anyMatch(list ->
                        list.stream().anyMatch(td -> td != null && !td.isDead()));
                if (!anyAlive) {
                    // cancel animator
                    animatorRunning = false;
                    this.cancel();
                }
            }
        };
        animatorTask.runTaskTimer(Specialization.getInstance(), 0L, 1L);
    }

    /** --- Helpers --- **/
    private int getLineCount(String message) {
        int explicit = message.split("\n", -1).length;
        int approx = (int) Math.ceil(message.length() / 35.0);
        return Math.max(explicit, approx);
    }

    private String truncateMessage(String message) {

        if (message.length() <= maxChars) return message;
        return message.substring(0, maxChars - 3) + "...";
    }

    private void removeAllBubbles(Player player) {
        UUID uuid = player.getUniqueId();
        List<TextDisplay> bubbles = activeBubbles.remove(uuid);
        if (bubbles != null) {
            for (TextDisplay td : bubbles) removeBubble(td, player, true);
        }
        removeNameplate(player);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        e.joinMessage(null);
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        e.quitMessage(null);
        removeAllBubbles(e.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        removeAllBubbles(e.getPlayer());
    }

    private boolean tryHandleGlobalChat(Player player, String message) {
        String prefix = SpecializationConfig.getChatConfig().get("ANNOUNCEMENT_PREFIX", String.class);
        if (!message.startsWith(prefix) || !player.isOp()) return false;

        String actualMessage = message.substring(prefix.length()).trim();
        String format = SpecializationConfig.getChatConfig().get("ANNOUNCEMENT_FORMAT", String.class);
        Bukkit.getOnlinePlayers().forEach(p -> p.sendRichMessage(format.formatted(actualMessage)));
        return true;
    }

    private List<Player> getNearbyPlayers(Player player) {
        double radius = SpecializationConfig.getChatConfig().get("CHAT_RADIUS", Double.class);
        return player.getNearbyEntities(radius, radius, radius).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .toList();
    }
}
