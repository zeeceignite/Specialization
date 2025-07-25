package com.minecraftcivilizations.specialization.Listener;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceExtractEvent;

public class CraftingListener implements Listener {
    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(FurnaceExtractEvent event) {
        Player player = event.getPlayer();
        Material extracted = event.getItemType();
        int amount = event.getItemAmount();



    }
}
