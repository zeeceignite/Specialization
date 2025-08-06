package com.minecraftcivilizations.specialization.Mining;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class BreakBlockListener implements Listener {
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {

        AttributeInstance breakSpeedAttr = event.getPlayer().getAttribute(Attribute.BLOCK_BREAK_SPEED);
        if (breakSpeedAttr != null) {
            breakSpeedAttr.setBaseValue(1.0);
        }
    }
}
