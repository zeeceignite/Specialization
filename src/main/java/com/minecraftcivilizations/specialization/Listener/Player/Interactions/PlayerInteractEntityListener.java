package com.minecraftcivilizations.specialization.Listener.Player.Interactions;

import com.google.gson.reflect.TypeToken;
import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Listener.Player.PlayerDeathListener;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillLevel;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.Specialization;
import com.minecraftcivilizations.specialization.StaffTools.Debug;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import minecraftcivilizations.com.minecraftCivilizationsCore.Options.Pair;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerInteractEntityListener implements Listener {

    private static final Map<UUID, UUID> downedPlayerToHealer = new HashMap<>();

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
        Player p = e.getPlayer();
        if (p.isSneaking()) return;
        CustomPlayer customPlayer = CoreUtil.getPlayer(p.getUniqueId());

        if(e.getRightClicked() instanceof Boat){
            if(customPlayer.isDowned()) e.setCancelled(true);
            return;
        }

        if (e.getRightClicked() instanceof Player clickedPlayer) {
            if (customPlayer == null) return;
            if (customPlayer.getSkillLevel(SkillType.HEALER) > SkillLevel.JOURNEYMAN.getLevel()) {
                CustomPlayer downedPlayer = CoreUtil.getPlayer(clickedPlayer.getUniqueId());
                if (downedPlayer != null && downedPlayer.isDowned()) {
                    PlayerDeathListener.removeDownedArmorStand(clickedPlayer);
                    p.addPassenger(clickedPlayer);
                    downedPlayerToHealer.put(clickedPlayer.getUniqueId(), p.getUniqueId());

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (p.getPassengers().isEmpty()) {
                                p.removePotionEffect(PotionEffectType.SLOWNESS);
                                return;
                            }
                            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 2));
                        }
                    }.runTaskTimer(Specialization.getInstance(), 0, 1);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player healer = event.getPlayer();
        CustomPlayer customHealer = CoreUtil.getPlayer(healer.getUniqueId());

        if (customHealer == null) return;
        if (customHealer.getSkillLevel(SkillType.HEALER) <= SkillLevel.JOURNEYMAN.getLevel()) return;

        if (event.isSneaking() && !healer.getPassengers().isEmpty()) {
            for (org.bukkit.entity.Entity passenger : healer.getPassengers()) {
                if (passenger instanceof Player downedPlayer) {
                    CustomPlayer customDownedPlayer = CoreUtil.getPlayer(downedPlayer.getUniqueId());
                    if (customDownedPlayer != null && customDownedPlayer.isDowned()) {
                        downedPlayer.leaveVehicle();

                        Debug.broadcast("downed", "Player sneak downed PlayerInteractEntityListener");
                        org.bukkit.Location playerLoc = downedPlayer.getLocation();
                        org.bukkit.Location armorStandLoc = playerLoc.clone().subtract(0, 2, 0);

                        Interaction armorStand = downedPlayer.getWorld().spawn(armorStandLoc, Interaction.class);
//                        armorStand.setVisible(false);
                        armorStand.setResponsive(false);
                        armorStand.setInteractionWidth(0);
                        armorStand.setInteractionHeight(0);
                        armorStand.setInvulnerable(true);
                        armorStand.setGravity(false);
//                        armorStand.setCanPickupItems(false);
                        armorStand.setCustomNameVisible(false);
                        armorStand.setSilent(true);
                        armorStand.setCustomName("downed_" + downedPlayer.getUniqueId());

                        armorStand.addPassenger(downedPlayer);
                        break;
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDismount(EntityDismountEvent event) {
        if (!(event.getEntity() instanceof Player downedPlayer)) return;

        CustomPlayer customDownedPlayer = CoreUtil.getPlayer(downedPlayer.getUniqueId());
        if (customDownedPlayer == null || !customDownedPlayer.isDowned()) return;
        if (event.getDismounted() instanceof Player healer) {
            UUID latestHealerUUID = downedPlayerToHealer.get(downedPlayer.getUniqueId());
            if (latestHealerUUID != null) {
                Player latestHealer = downedPlayer.getServer().getPlayer(latestHealerUUID);

                if (latestHealer != null && latestHealer.isOnline() &&
                        latestHealer.getLocation().distance(downedPlayer.getLocation()) < 10) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (customDownedPlayer.isDowned() && downedPlayer.getVehicle() == null) {
                                latestHealer.addPassenger(downedPlayer);
                            }
                        }
                    }.runTaskLater(Specialization.getInstance(), 1L);
                    return;
                }
            }

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (customDownedPlayer.isDowned() && downedPlayer.getVehicle() == null) {
                        createArmorStandForDownedPlayer(downedPlayer);
                    }
                }
            }.runTaskLater(Specialization.getInstance(), 1L);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        CustomPlayer customPlayer = CoreUtil.getPlayer(player.getUniqueId());

        if (customPlayer != null && customPlayer.isDowned() && player.getVehicle() == null) {
            boolean beingCarried = false;
            for (Player onlinePlayer : player.getWorld().getPlayers()) {
                if (onlinePlayer.getPassengers().contains(player)) {
                    beingCarried = true;
                    break;
                }
            }

            if (!beingCarried) {
                UUID latestHealerUUID = downedPlayerToHealer.get(player.getUniqueId());
                if (latestHealerUUID != null) {
                    Player latestHealer = player.getServer().getPlayer(latestHealerUUID);
                    if (latestHealer != null && latestHealer.isOnline() &&
                            latestHealer.getLocation().distance(player.getLocation()) < 5) {
                        latestHealer.addPassenger(player);
                        return;
                    }
                }

                String expectedName = "downed_" + player.getUniqueId();
                for (ArmorStand armorStand : player.getWorld().getEntitiesByClass(ArmorStand.class)) {
                    if (expectedName.equals(armorStand.getCustomName()) &&
                            armorStand.getLocation().distance(player.getLocation()) < 5) {
                        armorStand.addPassenger(player);
                        break;
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerBreed(EntityBreedEvent e){
        if(e.getBreeder() instanceof Player player) {
            CustomPlayer cPlayer = CoreUtil.getPlayer(player);
            Integer level = SpecializationConfig.getFarmerConfig().get("FARMER_BREED_LEVEL_" + e.getMother().getType(), Integer.class);
            if(level != null && cPlayer.getSkillLevel(SkillType.FARMER) < level){
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerTame(EntityTameEvent e){
        if(e.getOwner() instanceof Player player) {
            CustomPlayer cPlayer = CoreUtil.getPlayer(player);
            Pair<SkillType,Integer> level = SpecializationConfig.getTameableConfig().get("TAME_" + e.getEntity().getType(), new TypeToken<>(){});
            if(level != null && cPlayer.getSkillLevel(level.firstValue()) < level.secondValue()){
                e.setCancelled(true);
            }
        }
    }

    private void createArmorStandForDownedPlayer(Player downedPlayer) {
        org.bukkit.Location playerLoc = downedPlayer.getLocation();
        org.bukkit.Location armorStandLoc = playerLoc.clone().subtract(0, SpecializationConfig.getDownedConfig().get("OFFSET_TO_GROUND", Double.class), 0);

        ArmorStand armorStand = downedPlayer.getWorld().spawn(armorStandLoc, ArmorStand.class);
        armorStand.setVisible(false);
        armorStand.setInvulnerable(true);
        armorStand.setGravity(false);
        armorStand.setCanPickupItems(false);
        armorStand.setCustomNameVisible(false);
        armorStand.setSilent(true);
        armorStand.setCustomName("downed_" + downedPlayer.getUniqueId());

        armorStand.addPassenger(downedPlayer);
    }
}