package com.minecraftcivilizations.specialization.Listener.Player;

import com.comphenix.protocol.wrappers.Pair;
import com.google.gson.reflect.TypeToken;
import com.minecraftcivilizations.specialization.Analytics.AnalyticsData;
import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.Specialization;
import com.minecraftcivilizations.specialization.StaffTools.Debug;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

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

        Player killer = event.getPlayer().getKiller();
        if (killer == null) {
            playerActuallyDied(event.getPlayer());
            return;
        }

        // Player was killed by another player — trigger downed state
        customPlayer.setDowned(true);
        event.getPlayer().setHealth(10);
        applyDownedEffects(event.getPlayer());
        event.setCancelled(true);

        Location playerLoc = event.getPlayer().getLocation();
        Location armorStandLoc = playerLoc.clone();//.subtract(0, SpecializationConfig.getDownedConfig()
//                .get("OFFSET_TO_GROUND", Double.class), 0);

//        Display marker = event.getPlayer().getWorld().spawn(armorStandLoc, Display.class);
        Interaction marker = event.getPlayer().getWorld().spawn(armorStandLoc, Interaction.class);

        marker.setResponsive(false);
        marker.setInteractionWidth(0);
        marker.setInteractionHeight(0);
        Debug.broadcast("downed", "INTERACTION spawned: 61");
//        armorStand.setVisible(false);
        marker.setInvulnerable(true);
        marker.setGravity(false);
//        armorStand.setCanPickupItems(false);
        marker.setCustomNameVisible(false);
        marker.setSilent(true);
        marker.setCustomName("downed_" + event.getPlayer().getUniqueId());
        marker.addPassenger(event.getPlayer());
    }

    public static void removeDownedArmorStand(Player player) {
        if (player.getVehicle() instanceof Marker marker) {
            String expectedName = "downed_" + player.getUniqueId();
            if (expectedName.equals(marker.getCustomName())) {
                player.leaveVehicle();
                marker.remove();
            }
        }
    }

    public static void restoreDownedState(Player player, CustomPlayer customPlayer) {
        try {
            java.lang.reflect.Field downedField = CustomPlayer.class.getDeclaredField("isDowned");
            downedField.setAccessible(true);
            downedField.set(customPlayer, true);
        } catch (Exception e) {
            customPlayer.setDowned(true);
        }

        customPlayer.setLastDowned(System.currentTimeMillis());
        player.setHealth(10);

        PlayerDeathListener listener = new PlayerDeathListener();
        listener.applyDownedEffects(player);

        Location playerLoc = player.getLocation();
        Location armorStandLoc = playerLoc.clone().subtract(0,
                SpecializationConfig.getDownedConfig().get("OFFSET_TO_GROUND", Double.class), 0);

        Interaction marker = player.getWorld().spawn(armorStandLoc, Interaction.class);
        marker.setResponsive(false);
        marker.setInteractionWidth(0);
        marker.setInteractionHeight(0);
        Debug.broadcast("downed", "marker spawned line 102");
//        armorStand.setVisible(false);
        marker.setInvulnerable(true);
        marker.setGravity(false);
//        armorStand.setCanPickupItems(false);
        marker.setCustomNameVisible(false);
        marker.setSilent(true);
        marker.setCustomName("downed_" + player.getUniqueId());
        marker.addPassenger(player);
    }

    public void playerActuallyDied(Player player) {
        CustomPlayer customPlayer = CoreUtil.getPlayer(player);
        customPlayer.getAnalyticPlayerData().incrementDeathsThisPeriod();

        double deathReducedMaxHealth = SpecializationConfig.getHealthConfig().get("DEATH_REDUCED_MAX_HEALTH", Double.class);
        if (SpecializationConfig.getHealthConfig().get("HEALTH_ENABLED", Boolean.class) && deathReducedMaxHealth > 0) {
            if (customPlayer.getSkillLevel(SkillType.BUILDER) >= 1
                    || customPlayer.getSkillLevel(SkillType.HEALER) >= 1
                    || customPlayer.getSkillLevel(SkillType.BLACKSMITH) >= 1
                    || customPlayer.getSkillLevel(SkillType.GUARDSMAN) >= 1
                    || customPlayer.getSkillLevel(SkillType.MINER) >= 1
                    || customPlayer.getSkillLevel(SkillType.LIBRARIAN) >= 1
                    || customPlayer.getSkillLevel(SkillType.FARMER) >= 1) {
                Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH))
                        .setBaseValue(deathReducedMaxHealth);
            }
        }

        if (player.getLastDamageCause() == null) return;

        EntityDamageEvent.DamageCause cause = player.getLastDamageCause().getCause();
        AnalyticsData.deaths.putIfAbsent(cause, 0);
        AnalyticsData.deaths.put(cause, AnalyticsData.deaths.get(cause) + 1);

        customPlayer.getSkills().forEach(skill ->
                customPlayer.addSkillXp(skill.getSkillType(), -skill.getXp(), null, true, false));

//        Bukkit.getScheduler().runTaskLater(Specialization.getInstance(), () ->
//                player.kick(Component.text("You died.")), 30);
    }

    public void applyDownedEffects(Player player) {
        // Apply all configured effects EXCEPT Weakness (we’ll add it manually)
        for (PotionEffectType potionEffectType : Registry.MOB_EFFECT) {
            if (potionEffectType == PotionEffectType.WEAKNESS) continue;

            try {
                String effectKey = potionEffectType.getKey().getKey();
                Pair<Double, Double> effectData = SpecializationConfig.getDownedConfig()
                        .get(effectKey, new TypeToken<Pair<Double, Double>>() {});
                if (effectData != null && effectData.getFirst() != null && effectData.getSecond() != null) {
                    int duration = effectData.getFirst().intValue();
                    int amplifier = effectData.getSecond().intValue();
                    if (duration > 0) {
                        player.addPotionEffect(new PotionEffect(potionEffectType, duration, amplifier, false, false), true);
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to apply downed effect " + potionEffectType.getKey() + ": " + e.getMessage());
            }
        }


        int duration = 600;
        int amplifier = 255;
        Pair<Double, Double> weaknessData = SpecializationConfig.getDownedConfig()
                .get("weakness", new TypeToken<Pair<Double, Double>>() {});
        if (weaknessData != null) {
            if (weaknessData.getFirst() != null && weaknessData.getFirst() > 0)
                duration = weaknessData.getFirst().intValue();
            if (weaknessData.getSecond() != null)
                amplifier = weaknessData.getSecond().intValue();
        }

        player.removePotionEffect(PotionEffectType.WEAKNESS);
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, Math.max(duration, 20), amplifier, false, false), true);
    }


    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onDownedPlayerDealDamageLowest(EntityDamageByEntityEvent event) {
        if (isDamageFromDownedPlayer(event)) {
            event.setCancelled(true);
            Player attacker = getAttackingPlayer(event);
            if (attacker != null) attacker.sendMessage("§cYou are downed and cannot attack!");
        }
    }


    @EventHandler(ignoreCancelled = false, priority = EventPriority.MONITOR)
    public void onDownedPlayerDealDamageMonitor(EntityDamageByEntityEvent event) {
        if (!event.isCancelled() && isDamageFromDownedPlayer(event)) {
            event.setCancelled(true);
            Player attacker = getAttackingPlayer(event);
            if (attacker != null) attacker.sendMessage("§cYou are downed and cannot attack!");
        }
    }


    private boolean isDamageFromDownedPlayer(EntityDamageByEntityEvent event) {
        Player attacker = getAttackingPlayer(event);
        if (attacker == null) return false;
        CustomPlayer cp = com.minecraftcivilizations.specialization.util.CoreUtil.getPlayer(attacker);
        return cp != null && cp.isDowned();
    }

    private Player getAttackingPlayer(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player p) return p;
        if (event.getDamager() instanceof org.bukkit.entity.Projectile proj && proj.getShooter() instanceof Player p) return p;
        return null;
    }
}
