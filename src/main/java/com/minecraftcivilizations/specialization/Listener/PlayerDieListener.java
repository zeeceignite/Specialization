package com.minecraftcivilizations.specialization.Listener;

import com.minecraftcivilizations.specialization.Analytics.AnalyticsData;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDieListener implements Listener {

    @EventHandler
    public void die(PlayerDeathEvent e) {
        CustomPlayer.AnalyticPlayerData data = CoreUtil.getPlayer(e.getPlayer()).getAnalyticPlayerData();
        data.setDeaths(data.getDeaths() + 1);
        if(e.getEntity().getLastDamageCause() == null) return;
        EntityDamageEvent.DamageCause cause = e.getEntity().getLastDamageCause().getCause();
        AnalyticsData.deaths.putIfAbsent(cause, 0);
        AnalyticsData.deaths.put(cause, AnalyticsData.deaths.get(cause) + 1);
    }
}
