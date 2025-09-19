package com.minecraftcivilizations.specialization.Listener.Player.Interactions;

import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;

public class FishingListener implements Listener {

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        if(event.getCaught() instanceof Item) {
            Item item = (Item) event.getCaught();
            if(item.getItemStack().getType().toString().toLowerCase().matches("(Enchanted Book|Bow)/i")){
                if(Math.random() > 0.0005){
                    event.setCancelled(true);
                }
            }
        }
    }

}
