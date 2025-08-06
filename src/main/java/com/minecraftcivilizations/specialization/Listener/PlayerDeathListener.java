package com.minecraftcivilizations.specialization.Listener;

import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PlayerDeathListener implements Listener {
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        CustomPlayer player = CoreUtil.getPlayer(event.getPlayer().getUniqueId());
        if (player == null || (player.isDowned() && player.isDownedTimeout())) {
            if (player != null) {
                player.setDowned(false);
                removeDownedArmorStand(event.getPlayer());
            }
            return;
        }

        player.setDowned(true);
        event.getPlayer().setHealth(10);
        event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 2400, 0, true, false));
        event.setCancelled(true);

        Location playerLoc = event.getPlayer().getLocation();
        Location armorStandLoc = playerLoc.clone().subtract(0, 2, 0);

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
}