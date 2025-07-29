package com.minecraftcivilizations.specialization.Listener;

import com.minecraftcivilizations.specialization.Config.Config;
import com.minecraftcivilizations.specialization.Specialization;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class CrossBowListener implements Listener {

    Plugin plugin = Specialization.getInstance();
    Map<Arrow, Location> arrowLocations = new HashMap<>();


    @EventHandler
    public void onCrossBowShoot(EntityShootBowEvent bowEvent) {
        assert bowEvent.getBow() != null;
        Arrow arrow = (Arrow) bowEvent.getProjectile();


        if (arrow.isShotFromCrossbow()) {
            arrow.setVelocity(arrow.getVelocity().multiply(1.6));
            if (bowEvent.getBow().containsEnchantment(Enchantment.MULTISHOT)) {
                arrow.setVelocity(arrow.getVelocity().multiply(1.3));
            } else if (bowEvent.getBow().containsEnchantment(Enchantment.PIERCING)) {
                arrow.setVelocity(arrow.getVelocity().multiply(2.5));
            } else if (bowEvent.getBow().containsEnchantment(Enchantment.QUICK_CHARGE)) {
                arrow.setVelocity(arrow.getVelocity().multiply(1.3));
            }
        }
    }

}
