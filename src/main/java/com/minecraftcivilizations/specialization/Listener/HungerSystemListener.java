package com.minecraftcivilizations.specialization.Listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HungerSystemListener implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> lastMoveTime = new HashMap<>();
    private final Map<UUID, PlayerActivity> playerActivity = new HashMap<>();

    // Hunger drain rates (higher = more hunger loss)
    private static final double SPRINTING_DRAIN = 2;
    private static final double WALKING_DRAIN = 0.5;
    private static final double CRAWLING_DRAIN = 0.2;
    private static final double IDLE_DRAIN = 0.1; // Very small drain when standing still

    // Time intervals (in ticks - 20 ticks = 1 second)
    private static final long DRAIN_INTERVAL = 100; // Drain every 5 seconds
    private static final long IDLE_CHECK_TIME = 100; // 5 seconds of no movement = idle

    public HungerSystemListener(JavaPlugin plugin) {
        this.plugin = plugin;
        startHungerDrainTask();
    }

    private enum PlayerActivity {
        SPRINTING,
        WALKING,
        CRAWLING,
        IDLE
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Check if player actually moved (not just looking around)
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return; // Player didn't actually move, just looked around
        }

        lastMoveTime.put(playerId, System.currentTimeMillis());

        // Determine activity based on player state
        PlayerActivity activity;
        if (player.isSprinting()) {
            activity = PlayerActivity.SPRINTING;
        } else if (player.isSneaking()) {
            activity = PlayerActivity.CRAWLING;
        } else {
            activity = PlayerActivity.WALKING;
        }

        playerActivity.put(playerId, activity);
    }

    @EventHandler
    public void onPlayerToggleSprint(PlayerToggleSprintEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (event.isSprinting()) {
            playerActivity.put(playerId, PlayerActivity.SPRINTING);
        } else {
            // If they stop sprinting but are still moving, they're walking
            // This will be updated by the move event if needed
            if (playerActivity.get(playerId) == PlayerActivity.SPRINTING) {
                playerActivity.put(playerId, PlayerActivity.WALKING);
            }
        }

        lastMoveTime.put(playerId, System.currentTimeMillis());
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (event.isSneaking()) {
            playerActivity.put(playerId, PlayerActivity.CRAWLING);
        } else {
            // If they stop sneaking, assume walking (will be updated by move event)
            if (playerActivity.get(playerId) == PlayerActivity.CRAWLING) {
                playerActivity.put(playerId, PlayerActivity.WALKING);
            }
        }

        lastMoveTime.put(playerId, System.currentTimeMillis());
    }

    private void startHungerDrainTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();

                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    UUID playerId = player.getUniqueId();

                    // Determine if player is idle
                    Long lastMove = lastMoveTime.get(playerId);
                    boolean isIdle = lastMove == null ||
                            (currentTime - lastMove) > (IDLE_CHECK_TIME * 50); // Convert ticks to milliseconds

                    PlayerActivity activity;
                    if (isIdle) {
                        activity = PlayerActivity.IDLE;
                    } else {
                        activity = playerActivity.getOrDefault(playerId, PlayerActivity.IDLE);
                    }

                    // Apply hunger drain based on activity
                    drainHunger(player, activity);
                }
            }
        }.runTaskTimer(plugin, DRAIN_INTERVAL, DRAIN_INTERVAL);
    }

    private void drainHunger(Player player, PlayerActivity activity) {
        double currentFoodLevel = player.getFoodLevel();
        double drainAmount = getDrainAmount(activity);

        // Directly drain food level
        double newFoodLevel = Math.max(0, currentFoodLevel - drainAmount);
        player.setFoodLevel((int) newFoodLevel);
    }

    private double getDrainAmount(PlayerActivity activity) {
        switch (activity) {
            case SPRINTING:
                return SPRINTING_DRAIN;
            case WALKING:
                return WALKING_DRAIN;
            case CRAWLING:
                return CRAWLING_DRAIN;
            case IDLE:
                return IDLE_DRAIN;
            default:
                return IDLE_DRAIN;
        }
    }

    // Clean up data when player leaves
    public void removePlayer(UUID playerId) {
        lastMoveTime.remove(playerId);
        playerActivity.remove(playerId);
    }

    // Getters for configuration/testing
    public double getSprintingDrain() { return SPRINTING_DRAIN; }
    public double getWalkingDrain() { return WALKING_DRAIN; }
    public double getCrawlingDrain() { return CRAWLING_DRAIN; }
    public double getIdleDrain() { return IDLE_DRAIN; }

    // Method to update drain rates if needed (for config-based rates)
    public void setDrainRates(double sprinting, double walking, double crawling, double idle) {
        // You could implement dynamic rate changes here if needed
        // For now, rates are constants but this method provides extensibility
    }
}