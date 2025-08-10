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
    }
}
