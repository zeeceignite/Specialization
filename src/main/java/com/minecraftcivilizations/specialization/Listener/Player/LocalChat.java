package com.minecraftcivilizations.specialization.Listener.Player;

import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Specialization;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.*;

public class LocalChat implements Listener {

    // Base height above head
    private static final float BASE_HEIGHT = 0.6f;

    // Default vertical spacing between messages (for 1-line msgs)
    private static final float SINGLE_LINE_SPACING = 0.30f;

    // Additional spacing per line when message is multi-line
    private static final float MULTILINE_SPACING = 0.30f;
    //Color for ( ) around msgs
    private static final String PAREN_COLOR = "<#555555>"; // dark grey
    // Chat bubble text color
    private static final String CHAT_BUBBLE_COLOR = "<#f5f2c8>"; // change inline here

    private static final int MAX_BUBBLES = 3;
    private static final long LIFETIME_TICKS = 20L * 10;
    private static final int ANIMATION_DELAY = 1;

    private final Map<UUID, List<TextDisplay>> activeBubbles = new HashMap<>();
    private final Map<UUID, TextDisplay> activeNames = new HashMap<>();
    private final Map<TextDisplay, String> bubbleMessages = new HashMap<>();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        event.setCancelled(true);

        Player sender = event.getPlayer();
        String message = MiniMessage.miniMessage().stripTags(event.getMessage().trim());
        if (tryHandleGlobalChat(sender, message)) return;

        String format = SpecializationConfig.getChatConfig().get("DEFAULT_FORMAT", String.class);
        Bukkit.getScheduler().runTask(Specialization.getInstance(), () -> {
            for (Player near : getNearbyPlayers(sender))
                near.sendRichMessage(format.formatted(sender.getName(), message));
            sender.sendRichMessage(format.formatted(sender.getName(), message));
            showChatBubble(sender, message);
        });
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
            for (TextDisplay td : bubbles) removeBubble(td, player, true);
            bubbles.clear();
        } else if (totalChars > 150) { // remove only oldest
            if (!bubbles.isEmpty()) {
                TextDisplay oldest = bubbles.remove(0);
                removeBubble(oldest, player, true);
            }
        }

        // --- Enforce MAX_BUBBLES ---
        while (bubbles.size() >= MAX_BUBBLES) {
            TextDisplay oldest = bubbles.remove(0);
            removeBubble(oldest, player, true);
        }

        // --- Shift remaining bubbles upward ---
        for (TextDisplay td : bubbles) {
            if (td.isDead()) continue;
            Transformation t = td.getTransformation();
            Vector3f pos = new Vector3f(t.getTranslation());
            pos.y += bubbleHeight;
            t.getTranslation().set(pos);
            td.setTransformation(t);
        }

        // --- Spawn new bubble ---
        TextDisplay td = spawnBubble(player, BASE_HEIGHT);
        bubbleMessages.put(td, message); // track text
        animateText(td, message);
        player.addPassenger(td);
        bubbles.add(td);

        // --- Schedule fade-out normally ---
        Bukkit.getScheduler().runTaskLater(Specialization.getInstance(), () -> {
            bubbles.remove(td);
            bubbleMessages.remove(td);
            removeBubble(td, player, false);
        }, LIFETIME_TICKS);
    }



    /** --- Fade + cleanup --- **/

    private void removeBubble(TextDisplay td, Player player, boolean forceImmediate) {
        bubbleMessages.remove(td); // remove from tracking

        if (td.isDead()) {
            checkRemoveNameplate(player);
            return;
        }

        if (forceImmediate) {
            td.remove();
            Bukkit.getScheduler().runTaskLater(Specialization.getInstance(), () -> checkRemoveNameplate(player), 1L);
            return;
        }

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

    /** --- Nameplate --- **/
    private void spawnNameplate(Player sender) {
        UUID uuid = sender.getUniqueId();
        TextDisplay nameplate = sender.getWorld().spawn(sender.getLocation(), TextDisplay.class, td -> {
            td.text(MiniMessage.miniMessage().deserialize(sender.getName()));
            td.setBillboard(Display.Billboard.CENTER);
            td.setShadowed(false);
            td.setSeeThrough(false);
            td.setViewRange(32f);
            td.setPersistent(false);
            td.setInterpolationDuration(0);
            td.setBrightness(new Display.Brightness(14, 14));

            Vector3f translation = new Vector3f(0, 0.27f, 0);
            Vector3f scale = new Vector3f(1, 1, 1);
            AxisAngle4f rotation = new AxisAngle4f(0, 0, 1, 0);
            td.setTransformation(new Transformation(translation, rotation, scale, rotation));
        });

        activeNames.put(uuid, nameplate);
        sender.addPassenger(nameplate);
        //so the player doesn't see their own nameplate
        sender.hideEntity(Specialization.getInstance(), nameplate);
    }

    private void removeNameplate(Player player) {
        UUID uuid = player.getUniqueId();
        TextDisplay nameplate = activeNames.remove(uuid);
        if (nameplate != null && !nameplate.isDead()) nameplate.remove();
    }

    /** --- Text display creation --- **/
    private TextDisplay spawnBubble(Player player, float yOffset) {
        return player.getWorld().spawn(player.getLocation(), TextDisplay.class, td -> {
            // Start with empty text but colored properly
            td.text(MiniMessage.miniMessage().deserialize(CHAT_BUBBLE_COLOR + ""));

            // Dark background
            //td.setDefaultBackground(false);
            //td.setBackgroundColor(Color.fromRGB(20, 20, 20)); // dark gray

            td.setBillboard(Display.Billboard.CENTER);
            td.setShadowed(true);
            td.setSeeThrough(false);
            td.setViewRange(32f);
            td.setPersistent(false);
            td.setInterpolationDuration(1);
            td.setBrightness(new Display.Brightness(10, 10));

            Vector3f translation = new Vector3f(0, yOffset, 0);
            Vector3f scale = new Vector3f(1, 1, 1);
            AxisAngle4f rotation = new AxisAngle4f(0, 0, 1, 0);
            td.setTransformation(new Transformation(translation, rotation, scale, rotation));
        });
    }


    /** --- Typing animation --- **/
    private void animateText(TextDisplay td, String message) {
        Bukkit.getScheduler().runTaskAsynchronously(Specialization.getInstance(), () -> {
            StringBuilder builder = new StringBuilder();
            for (char c : message.toCharArray()) {
                builder.append(c);
                String partial = PAREN_COLOR + "(" + CHAT_BUBBLE_COLOR + builder + PAREN_COLOR + ")";
                Bukkit.getScheduler().runTask(Specialization.getInstance(), () -> {
                    if (!td.isDead()) td.text(MiniMessage.miniMessage().deserialize(partial));
                });
                try { Thread.sleep(ANIMATION_DELAY * 10L); } catch (InterruptedException ignored) {}
            }
        });
    }


    /** --- Helpers --- **/
    private int getLineCount(String message) {
        int explicit = message.split("\n", -1).length;
        int approx = (int) Math.ceil(message.length() / 40.0);
        return Math.max(explicit, approx);
    }

    private String truncateMessage(String message) {
        int maxChars = 135;
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
