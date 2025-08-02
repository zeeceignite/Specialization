package com.minecraftcivilizations.specialization.Listener;

import com.minecraftcivilizations.specialization.Distance.Town;
import com.minecraftcivilizations.specialization.Distance.TownManager;
import com.minecraftcivilizations.specialization.Reinforcement.ReinforcementManager;
import com.minecraftcivilizations.specialization.Specialization;
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

import java.util.List;

import static com.minecraftcivilizations.specialization.Skill.Skill.mapValue;

public class MoveListener implements Listener {
    @EventHandler
    public void onMove(PlayerMoveEvent event) {

        ReinforcementManager.checkForReinforcements(event.getPlayer());

        Location from = event.getFrom();

        if (!event.hasChangedBlock()) return;

        List<Town> towns = TownManager.getTowns();

        if(towns.isEmpty()) return;

        double minDistanceSq = Double.MAX_VALUE;
        for (Town town : TownManager.getTowns()) {
            if (town.getCenterLocation().distanceSquared(from) < minDistanceSq) {
                minDistanceSq = town.getCenterLocation().distanceSquared(from);
            }
        }

        double minDistance = Math.sqrt(minDistanceSq);

        BossBar bar = Bukkit.getBossBar(new NamespacedKey(Specialization.getInstance(), "distanceBar"));
        if (minDistance > 100) {
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
