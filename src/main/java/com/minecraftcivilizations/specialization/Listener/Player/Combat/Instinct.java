package com.minecraftcivilizations.specialization.Listener.Player.Combat;

import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.util.LocatorBarManager;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class Instinct {

    private final Plugin plugin;
    private static final Map<UUID, Long> lastDetectionTime = new HashMap<>();

    public Instinct(Plugin plugin) {
        this.plugin = plugin;
        Bukkit.getLogger().info("Instinct class initialized");
    }

    /**
     * Called by BreakBlockMobGoal when a mob starts breaking a block
     * This is the main entry point for the Instinct system
     */
    public static void onMobStartBreakingBlock(Monster mob) {
        Bukkit.getLogger().info("onMobStartBreakingBlock called for mob: " + mob.getType() + " at " + mob.getLocation());

        boolean instinctEnabled = SpecializationConfig.getInstinctConfig().get("INSTINCT_ENABLED", Boolean.class);
        Bukkit.getLogger().info("Instinct enabled: " + instinctEnabled);
        
        if (!instinctEnabled) {
            Bukkit.getLogger().info("Instinct is disabled, returning");
            return;
        }

        int onlinePlayersCount = Bukkit.getOnlinePlayers().size();
        Bukkit.getLogger().info("Checking " + onlinePlayersCount + " online players");

        for (Player player : Bukkit.getOnlinePlayers()) {
            CustomPlayer customPlayer = CoreUtil.getPlayer(player);
            int guardsmanLevel = customPlayer.getSkillLevel(SkillType.GUARDSMAN);
            
            Bukkit.getLogger().info("Player " + player.getName() + " has Guardsman level: " + guardsmanLevel);

            if (guardsmanLevel < 1) {
                Bukkit.getLogger().info("Player " + player.getName() + " has insufficient Guardsman level, skipping");
                continue;
            }

            double detectionRadius = getDetectionRadius(guardsmanLevel);
            Bukkit.getLogger().info("Detection radius for level " + guardsmanLevel + ": " + detectionRadius);

            boolean sameWorld = player.getWorld().equals(mob.getWorld());
            double distance = sameWorld ? player.getLocation().distance(mob.getLocation()) : Double.MAX_VALUE;
            
            Bukkit.getLogger().info("Player " + player.getName() + " - Same world: " + sameWorld + ", Distance: " + distance);

            if (sameWorld && distance <= detectionRadius) {
                Bukkit.getLogger().info("Player " + player.getName() + " is within range, applying detection");
                applyInstinctDetection(mob, player);
            } else {
                Bukkit.getLogger().info("Player " + player.getName() + " is out of range, skipping");
            }
        }
    }

    private static double getDetectionRadius(int guardsmanLevel) {
        double radius;
        if (guardsmanLevel >= 3) {
            radius = SpecializationConfig.getInstinctConfig().get("INSTINCT_DETECTION_RADIUS_LEVEL_3", Double.class);
        } else if (guardsmanLevel >= 2) {
            radius = SpecializationConfig.getInstinctConfig().get("INSTINCT_DETECTION_RADIUS_LEVEL_2", Double.class);
        } else {
            radius = SpecializationConfig.getInstinctConfig().get("INSTINCT_DETECTION_RADIUS_LEVEL_1", Double.class);
        }
        Bukkit.getLogger().info("getDetectionRadius for level " + guardsmanLevel + ": " + radius);
        return radius;
    }

    private static void applyInstinctDetection(LivingEntity mob, Player guardsman) {
        UUID mobId = mob.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        Bukkit.getLogger().info("applyInstinctDetection called for mob " + mob.getType() + " and player " + guardsman.getName());
        
        if (lastDetectionTime.containsKey(mobId) && 
            currentTime - lastDetectionTime.get(mobId) < 5000) {
            Bukkit.getLogger().info("Mob " + mob.getType() + " was recently detected, skipping (spam prevention)");
            return;
        }

        lastDetectionTime.put(mobId, currentTime);
        Bukkit.getLogger().info("Added mob to detection time map");

        int detectionDuration = SpecializationConfig.getInstinctConfig().get("INSTINCT_GLOW_DURATION_TICKS", Integer.class);
        Bukkit.getLogger().info("Detection duration: " + detectionDuration + " ticks");

        LocatorBarManager locatorBarManager = LocatorBarManager.getInstance();
        if (locatorBarManager != null) {
            Bukkit.getLogger().info("LocatorBarManager found, granting temporary visibility");
            locatorBarManager.grantTemporaryVisibility(guardsman, mob, detectionDuration);
        } else {
            Bukkit.getLogger().warning("LocatorBarManager is null!");
        }
        
        Bukkit.getScheduler().runTaskLater(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("Specialization")), () -> {
            lastDetectionTime.remove(mobId);
            Bukkit.getLogger().info("Removed mob from detection time map after " + detectionDuration + " ticks");
        }, detectionDuration + 20L);
    }
}
