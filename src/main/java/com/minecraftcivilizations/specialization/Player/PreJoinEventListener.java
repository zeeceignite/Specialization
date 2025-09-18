package com.minecraftcivilizations.specialization.Player;

import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

public class PreJoinEventListener implements Listener {
    @EventHandler
    public void onPreJoin(AsyncPlayerPreLoginEvent event) {
        onPreLogin(event.getPlayerProfile(), event.getUniqueId());
    }
    private void onPreLogin(PlayerProfile playerProfile, UUID uuid){
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
    }
}
