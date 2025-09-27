package com.minecraftcivilizations.specialization.Listener.Blocks;

import com.minecraftcivilizations.specialization.Reinforcement.ReinforcementManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.entity.FallingBlock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReinforcementProtectionListener implements Listener {

    private final Map<Block, Boolean> temporaryReinforcementStorage = new HashMap<>();

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof FallingBlock)) {
            return;
        }

        Block block = event.getBlock();

        if (ReinforcementManager.isReinforced(block)) {
            event.setCancelled(true);
            return;
        }

        FallingBlock fallingBlock = (FallingBlock) event.getEntity();
        Material blockType = fallingBlock.getBlockData().getMaterial();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockStartFalling(org.bukkit.event.block.BlockPhysicsEvent event) {
        Block block = event.getBlock();

        if (isFallingBlockType(block.getType()) && ReinforcementManager.isReinforced(block)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        List<Block> blocks = event.getBlocks();
        for (Block block : blocks) {
            if (ReinforcementManager.isReinforced(block)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * Handle piston retraction - when sticky pistons pull blocks
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        List<Block> blocks = event.getBlocks();

        for (Block block : blocks) {
            if (ReinforcementManager.isReinforced(block)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        // Remove reinforced blocks from the explosion block list
        event.blockList().removeIf(ReinforcementManager::isReinforced);
    }

    private boolean isFallingBlockType(Material material) {
        return material == Material.SAND ||
               material == Material.RED_SAND ||
               material == Material.GRAVEL ||
               material == Material.ANVIL ||
               material == Material.CHIPPED_ANVIL ||
               material == Material.DAMAGED_ANVIL ||
               material == Material.DRAGON_EGG ||
               material == Material.POINTED_DRIPSTONE ||
               material.name().contains("CONCRETE_POWDER");
    }
}
