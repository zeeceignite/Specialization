package com.minecraftcivilizations.specialization.Listener;

import com.minecraftcivilizations.specialization.Distance.Town;
import com.minecraftcivilizations.specialization.Distance.TownManager;
import com.minecraftcivilizations.specialization.Specialization;
import minecraftcivilizations.com.minecraftCivilizationsCore.Item.ItemUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import static com.minecraftcivilizations.specialization.Skill.Skill.mapValue;

public class MoveListener implements Listener {
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return; // No block movement, skip
        }

        Double minDistanceSq = null;
        double minDistance = 0;
        for (Town town : TownManager.getTowns()) {
            if (minDistanceSq == null || town.getCenterLocation().distanceSquared(from) < minDistanceSq) {
                minDistance = town.getCenterLocation().distance(from);
                minDistanceSq = town.getCenterLocation().distanceSquared(from);
            }
        }

        BossBar bar = Bukkit.getBossBar(new NamespacedKey(Specialization.getInstance(), "distanceBar"));
        if (minDistanceSq != null && minDistance > 100) {
            if (bar == null) {
                bar = Bukkit.createBossBar(new NamespacedKey(Specialization.getInstance(), "distanceBar"), "Distance from Town: " + minDistance + " blocks", BarColor.RED, BarStyle.SEGMENTED_10);
            }
            bar.setProgress(mapValue(Math.clamp(minDistance, 0, 1000), 0, 1000, 0, 1)); // 0.0 to 1.0
            bar.addFlag(BarFlag.DARKEN_SKY);
            bar.addFlag(BarFlag.CREATE_FOG);
            bar.addPlayer(event.getPlayer());
            bar.setTitle("Distance from Town: " + minDistance + " blocks");
            bar.setVisible(true);
        } else {
            if (bar != null) {
                bar.removePlayer(event.getPlayer());
            }
        }
    }
}
