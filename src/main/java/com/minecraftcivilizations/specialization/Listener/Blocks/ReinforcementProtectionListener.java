package com.minecraftcivilizations.specialization.Listener.Blocks;

import com.google.gson.Gson;
import com.minecraftcivilizations.specialization.Reinforcement.Reinforcement;
import com.minecraftcivilizations.specialization.Reinforcement.ReinforcementManager;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
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
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.*;

import static com.minecraftcivilizations.specialization.Reinforcement.ReinforcementManager.*;

public class ReinforcementProtectionListener implements Listener {

    private final Map<Location, Boolean> temporaryReinforcementStorage = new HashMap<>();

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof FallingBlock fallingBlock)) {
            return;
        }

        if (event.isCancelled()) {
            return;
        }

        Block block = event.getBlock();
        Location fallingBlockLocation = fallingBlock.getLocation();
        if (temporaryReinforcementStorage.containsKey(fallingBlockLocation)) {
            boolean isHeavy = temporaryReinforcementStorage.get(fallingBlockLocation);
            // Re-apply reinforcement WITHOUT awarding XP
            ReinforcementManager.   addReinforcementNoXp(block, isHeavy);
            temporaryReinforcementStorage.remove(fallingBlockLocation);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockStartFalling(org.bukkit.event.block.BlockPhysicsEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Block block = event.getBlock();
        if (isFallingBlockType(block.getType()) && ReinforcementManager.isReinforced(block)) {
            boolean isHeavy = isHeavilyReinforced(block);
            Bukkit.getScheduler().runTaskLater(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("Specialization")), () -> {
                block.getWorld().getEntitiesByClass(FallingBlock.class).stream()
                        .filter(fb -> fb.getLocation().distance(block.getLocation()) < 2.0)
                        .forEach(fb -> {
                            temporaryReinforcementStorage.put(fb.getLocation(), isHeavy);
                        });
            }, 1L);
            ReinforcementManager.removeReinforcement(block);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (event.isCancelled()) {
            return;
        }

        List<Block> blocks = event.getBlocks();
        Vector direction = event.getDirection().getDirection();
        Map<Block, Boolean> reinforcementData = new HashMap<>();

        for (Block block : blocks) {
            if (ReinforcementManager.isReinforced(block)) {
                boolean isHeavy = isHeavilyReinforced(block);
                reinforcementData.put(block, isHeavy);
                ReinforcementManager.removeReinforcement(block);
            }
        }
        if (!reinforcementData.isEmpty()) {
            Bukkit.getScheduler().runTaskLater(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("Specialization")), () -> {
                for (Map.Entry<Block, Boolean> entry : reinforcementData.entrySet()) {
                    Block originalBlock = entry.getKey();
                    boolean isHeavy = entry.getValue();
                    Location newLocation = originalBlock.getLocation().add(direction);
                    Block newBlock = newLocation.getBlock();
                    // Re-apply reinforcement WITHOUT awarding XP
                    ReinforcementManager.addReinforcementNoXp(newBlock, isHeavy);
                }
            }, 2L);
        }
    }

    // Local helper to add reinforcement without XP (keeps persistence only)
    public static boolean addReinforcementNoXp(Block block, boolean isHeavy) {
        Chunk chunk = block.getChunk();
        if (isHeavy && isHeavilyReinforced(block)) return false;
        if (!isHeavy && isLightlyReinforced(block)) return false;
        if (!isHeavy && isHeavilyReinforced(block)) return false;

        Set<Reinforcement> reinforcedBlocks = getReinforcedBlocks(chunk);
        if (reinforcedBlocks == null) {
            reinforcedBlocks = new HashSet<>();
        }

        boolean added = reinforcedBlocks.add(new Reinforcement(block.getLocation().toVector(), isHeavy));
        if (!added) return false;

        chunk.getPersistentDataContainer().set(namespacedKey, PersistentDataType.STRING,
                new Gson().toJson(reinforcedBlocks));

        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (event.isCancelled()) {
            return;
        }

        List<Block> blocks = event.getBlocks();
        Vector direction = event.getDirection().getDirection();
        Map<Block, Boolean> reinforcementData = new HashMap<>();

        for (Block block : blocks) {
            if (ReinforcementManager.isReinforced(block)) {
                boolean isHeavy = isHeavilyReinforced(block);
                reinforcementData.put(block, isHeavy);
                ReinforcementManager.removeReinforcement(block);
            }
        }
        if (!reinforcementData.isEmpty()) {
            Bukkit.getScheduler().runTaskLater(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("Specialization")), () -> {
                for (Map.Entry<Block, Boolean> entry : reinforcementData.entrySet()) {
                    Block originalBlock = entry.getKey();
                    boolean isHeavy = entry.getValue();
                    Location newLocation = originalBlock.getLocation().add(direction);
                    Block newBlock = newLocation.getBlock();
                    // Re-apply reinforcement WITHOUT awarding XP
                    ReinforcementManager.addReinforcementNoXp(newBlock, isHeavy);
                }
            }, 2L);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
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
