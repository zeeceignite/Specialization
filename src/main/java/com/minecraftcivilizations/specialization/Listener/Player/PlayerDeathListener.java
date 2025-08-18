package com.minecraftcivilizations.specialization.Listener.Player;

import com.comphenix.protocol.wrappers.Pair;
import com.google.gson.reflect.TypeToken;
import com.minecraftcivilizations.specialization.Analytics.AnalyticsData;
import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import org.bukkit.Location;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.Objects;

public class PlayerDeathListener implements Listener {
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        CustomPlayer customPlayer = CoreUtil.getPlayer(event.getPlayer().getUniqueId());
        if (customPlayer == null || (customPlayer.isDowned() && customPlayer.isDownedTimeout())) {
            if (customPlayer != null) {
                customPlayer.setDowned(false);
                removeDownedArmorStand(event.getPlayer());
                playerActuallyDied(event.getPlayer());
            }

            return;
        }

        // Only trigger downed state if killed by another player
        Player killer = event.getPlayer().getKiller();
        if (killer == null) {
            // Player was not killed by another player (e.g., mobs, environment, etc.)
            // Let them die normally
            playerActuallyDied(event.getPlayer());
            return;
        }

        // Player was killed by another player - trigger downed state
        customPlayer.setDowned(true);
        event.getPlayer().setHealth(10);

        applyDownedEffects(event.getPlayer());
        event.setCancelled(true);

        Location playerLoc = event.getPlayer().getLocation();
        Location armorStandLoc = playerLoc.clone().subtract(0, SpecializationConfig.getDownedConfig().get("OFFSET_TO_GROUND", Double.class), 0);

        ArmorStand armorStand = event.getPlayer().getWorld().spawn(armorStandLoc, ArmorStand.class);
        armorStand.setVisible(false);
        armorStand.setInvulnerable(true);
        armorStand.setGravity(false);
        armorStand.setCanPickupItems(false);
        armorStand.setCustomNameVisible(false);
        armorStand.setSilent(true);
        armorStand.setCustomName("downed_" + event.getPlayer().getUniqueId());

        armorStand.addPassenger(event.getPlayer());
    }

    public static void removeDownedArmorStand(Player player) {
        if (player.getVehicle() instanceof ArmorStand armorStand) {
            String expectedName = "downed_" + player.getUniqueId();
            if (expectedName.equals(armorStand.getCustomName())) {
                player.leaveVehicle();
                armorStand.remove();
            }
        }
    }

    public static void restoreDownedState(Player player, CustomPlayer customPlayer) {
        // Set the downed flag directly without starting the death timer
        // We need to set this manually to avoid triggering the death timer in setDowned()
        try {
            java.lang.reflect.Field downedField = CustomPlayer.class.getDeclaredField("isDowned");
            downedField.setAccessible(true);
            downedField.set(customPlayer, true);
        } catch (Exception e) {
            // Fallback: use setDowned but immediately cancel any timer
            customPlayer.setDowned(true);
        }
        
        customPlayer.setLastDowned(System.currentTimeMillis());
        
        // Set player health to downed health
        player.setHealth(10);
        
        // Apply downed effects
        PlayerDeathListener listener = new PlayerDeathListener();
        listener.applyDownedEffects(player);
        
        // Create and attach armor stand
        Location playerLoc = player.getLocation();
        Location armorStandLoc = playerLoc.clone().subtract(0, SpecializationConfig.getDownedConfig().get("OFFSET_TO_GROUND", Double.class), 0);
        
        ArmorStand armorStand = player.getWorld().spawn(armorStandLoc, ArmorStand.class);
        armorStand.setVisible(false);
        armorStand.setInvulnerable(true);
        armorStand.setGravity(false);
        armorStand.setCanPickupItems(false);
        armorStand.setCustomNameVisible(false);
        armorStand.setSilent(true);
        armorStand.setCustomName("downed_" + player.getUniqueId());
        
        armorStand.addPassenger(player);
        
        // Note: We intentionally do NOT start the death timer here to prevent infinite loops
        // The player will remain downed until healed or manually killed
    }

    public void playerActuallyDied(Player player){
        CustomPlayer customPlayer = CoreUtil.getPlayer(player);
        // Use new non-cumulative death tracking
        customPlayer.getAnalyticPlayerData().incrementDeathsThisPeriod();
        if(player.getLastDamageCause() == null) return;
        EntityDamageEvent.DamageCause cause = player.getLastDamageCause().getCause();
        AnalyticsData.deaths.putIfAbsent(cause, 0);
        AnalyticsData.deaths.put(cause, AnalyticsData.deaths.get(cause) + 1);
        customPlayer.getSkills().forEach(skill -> {
            customPlayer.addSkillXp(skill.getSkillType(), -skill.getXp());
        });
        
        // Reduce max health
        double deathReducedMaxHealth = SpecializationConfig.getHealthConfig().get("DEATH_REDUCED_MAX_HEALTH", Double.class);
        if (SpecializationConfig.getHealthConfig().get("HEALTH_ENABLED", Boolean.class) && deathReducedMaxHealth > 0) {
            boolean hasLevel = Arrays.stream(SkillType.values()).anyMatch(skill -> customPlayer.getSkillLevel(skill) > 1);
            if(hasLevel){
                double currentMaxHealth = Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getValue();
                Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(deathReducedMaxHealth);
            }
        }
    }

    public void applyDownedEffects(Player player) {
        for (PotionEffectType potionEffectType : Registry.EFFECT) {
            try {
                String effectKey = potionEffectType.getKey().getKey();
                Pair<Double, Double> effectData = SpecializationConfig.getDownedConfig().get(effectKey, new TypeToken<Pair<Double, Double>>(){});

                if (effectData != null && effectData.getFirst() != null && effectData.getSecond() != null) {
                    int duration = effectData.getFirst().intValue();
                    int amplifier = effectData.getSecond().intValue();

                    if (duration > 0) {
                        player.addPotionEffect(new PotionEffect(potionEffectType, duration, amplifier, false, false));
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to apply downed effect " + potionEffectType.getKey() + ": " + e.getMessage());
            }
        }
    }
}