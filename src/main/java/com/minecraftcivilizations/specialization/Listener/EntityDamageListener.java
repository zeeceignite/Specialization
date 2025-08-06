package com.minecraftcivilizations.specialization.Listener;

import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class EntityDamageListener implements Listener {
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim) || !(event.getDamager() instanceof Player attacker)) {
            return;
        }

        CustomPlayer victimCustomPlayer = CoreUtil.getPlayer(victim.getUniqueId());
        CustomPlayer attackerCustomPlayer = CoreUtil.getPlayer(attacker.getUniqueId());

        if (victimCustomPlayer == null || attackerCustomPlayer == null) {
            return;
        }

        boolean victimIsDowned = victimCustomPlayer.isDowned();
        boolean attackerIsDowned = attackerCustomPlayer.isDowned();

        if (victimIsDowned || attackerIsDowned) {
            event.setCancelled(true);
            return;
        }

        if (victim.getVehicle() == attacker || attacker.getVehicle() == victim) {
            event.setCancelled(true);
            return;
        }

        boolean healerCarryingVictim = attacker.getPassengers().contains(victim);
        boolean victimCarryingAttacker = victim.getPassengers().contains(attacker);

        if (healerCarryingVictim || victimCarryingAttacker) {
            event.setCancelled(true);
        }
    }
}