package com.minecraftcivilizations.specialization.Listener.Player;

import com.minecraftcivilizations.specialization.Command.EmoteManager;
import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import org.bukkit.GameMode;
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
    private final Map<UUID, Double> playerHungerBuffer = new HashMap<>();

    private static final double SPRINTING_DRAIN = SpecializationConfig.getHungerConfig().get("SPRINTING_DRAIN", Double.class);
    private static final double WALKING_DRAIN = SpecializationConfig.getHungerConfig().get("WALKING_DRAIN", Double.class);
    private static final double SWIMMING_DRAIN = SpecializationConfig.getHungerConfig().get("SWIMMING_DRAIN", Double.class);
    private static final double CROUCHING_DRAIN = SpecializationConfig.getHungerConfig().get("CROUCHING_DRAIN", Double.class);
    private static final double IDLE_DRAIN = SpecializationConfig.getHungerConfig().get("IDLE_DRAIN", Double.class);


    private static final long DRAIN_INTERVAL = SpecializationConfig.getHungerConfig().get("DRAIN_INTERVAL_IN_TICKS", Long.class);
    private static final long IDLE_CHECK_TIME = SpecializationConfig.getHungerConfig().get("IDLE_CHECK_TIME_IN_TICKS", Long.class);
    private final EmoteManager emoteCommand;

    public HungerSystemListener(JavaPlugin plugin, EmoteManager emoteCommand) {
        this.plugin = plugin;
        this.emoteCommand = emoteCommand;
        startHungerDrainTask();
    }

    private enum PlayerActivity {
        SPRINTING,
        WALKING,
        CROUCHING,
        SWIMMING,
        IDLE
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();


        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        lastMoveTime.put(playerId, System.currentTimeMillis());


        PlayerActivity activity;
        if (player.isSprinting()) {
            activity = PlayerActivity.SPRINTING;
        } else if (player.isSneaking()) {
            activity = PlayerActivity.CROUCHING;
        } else if (player.isSwimming()) {
            activity = PlayerActivity.SWIMMING;
        }
        else {
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
            playerActivity.put(playerId, PlayerActivity.CROUCHING);
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
        }.runTaskTimerAsynchronously(plugin, 0, DRAIN_INTERVAL * 2);
    }

    private void drainHunger(Player player, PlayerActivity activity) {
        // Prevent hunger drain while sleeping
        if (player.isSleeping()){
            return;
        }

        if (emoteCommand.isPlayerSitting(player) || player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        UUID playerId = player.getUniqueId();
        double currentFoodLevel = player.getFoodLevel();
        double drainAmount = getDrainAmount(activity);

        // Get or initialize the player's hunger buffer
        double hungerBuffer = playerHungerBuffer.getOrDefault(playerId, 0.0);

        // Add the drain amount to the buffer
        hungerBuffer += drainAmount;

        // Check if we have accumulated enough to drain at least 1 hunger point
        if (hungerBuffer >= 1.0) {
            int hungerPointsToDrain = (int) hungerBuffer;
            double newFoodLevel = Math.max(0, currentFoodLevel - hungerPointsToDrain);
            player.setFoodLevel((int) newFoodLevel);

            // Subtract the drained amount from buffer, keeping the remainder
            hungerBuffer -= hungerPointsToDrain;
        }

        // Store the updated buffer
        playerHungerBuffer.put(playerId, hungerBuffer);
    }


    private double getDrainAmount(PlayerActivity activity) {
        switch (activity) {
            case SPRINTING:
                return SPRINTING_DRAIN;
            case WALKING:
                return WALKING_DRAIN;
            case CROUCHING:
                return CROUCHING_DRAIN;
            case SWIMMING:
                return SWIMMING_DRAIN;
            default:
                return IDLE_DRAIN;
        }
    }
}