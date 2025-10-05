package com.minecraftcivilizations.specialization.Listener.Player;

import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Specialization;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class BedListener implements Listener {

    // NamespacedKey for storing bed ownership in PDC
    private static final NamespacedKey BED_OWNER_KEY = new NamespacedKey(Specialization.getInstance(), "bed_owner");

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractWithBed(PlayerInteractEvent event) {
        // Check if bed ownership is enabled
        if (!SpecializationConfig.getBedOwnershipConfig().get("BED_OWNERSHIP_ENABLED", Boolean.class)) {
            return;
        }

        // Only handle right-click on blocks
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || !isBed(clickedBlock.getType())) {
            return;
        }

        Player player = event.getPlayer();
        CustomPlayer customPlayer = CoreUtil.getPlayer(player.getUniqueId());
        if (customPlayer == null) {
            return;
        }

        // Get bed's PDC to check ownership
        Block headBlock = getBedHeadBlock(clickedBlock);
        if (headBlock == null) {
            return;
        }

        PersistentDataContainer bedPDC = headBlock.getChunk().getPersistentDataContainer();
        String locationKey = getLocationKey(headBlock.getLocation());
        String ownerUUIDString = bedPDC.get(new NamespacedKey(Specialization.getInstance(), locationKey), PersistentDataType.STRING);

        UUID currentOwner = null;
        if (ownerUUIDString != null) {
            try {
                currentOwner = UUID.fromString(ownerUUIDString);
            } catch (IllegalArgumentException e) {
                // Invalid UUID stored, clear it
                bedPDC.remove(new NamespacedKey(Specialization.getInstance(), locationKey));
            }
        }

        // Check if bed is already owned by another player
        if (currentOwner != null && !currentOwner.equals(player.getUniqueId())) {
            // Check if bed sharing is allowed
            if (!SpecializationConfig.getBedOwnershipConfig().get("ALLOW_BED_SHARING", Boolean.class)) {
                // Cancel the event completely to prevent any bed interaction
                event.setCancelled(true);
                String message = SpecializationConfig.getBedOwnershipConfig().get("BED_OWNERSHIP_MESSAGE", String.class);
                player.sendMessage(message);
                return;
            }
        }

        // If player already owns this bed, let them sleep normally (don't cancel)
        if (currentOwner != null && currentOwner.equals(player.getUniqueId())) {
            // Player owns this bed - allow normal sleeping behavior
            return;
        }

        // This is a new bed claim - clear any previous bed ownership for this player
        clearPlayerPreviousBed(player);

        // Claim the new bed by storing owner UUID in chunk PDC
        bedPDC.set(new NamespacedKey(Specialization.getInstance(), locationKey), PersistentDataType.STRING, player.getUniqueId().toString());

        String claimMessage = SpecializationConfig.getBedOwnershipConfig().get("BED_CLAIM_MESSAGE", String.class);
        player.sendMessage(claimMessage);

        Bukkit.getLogger().info("Player " + player.getName() + " claimed bed at " + headBlock.getLocation() + " using PDC");

        // Don't cancel the event - let the player sleep normally in their newly claimed bed
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        // Check if player is respawning from a bed (not world spawn)
        if (event.isBedSpawn()) {
            if (!SpecializationConfig.getBedOwnershipConfig().get("BED_RESPAWN_HUNGER_REDUCTION_ENABLED", Boolean.class)) {
                return;
            }
            int currentFoodLevel = player.getFoodLevel();
            
            // Get configurable divisor (default 3 means 1/3 of original hunger)
            int hungerDivisor = SpecializationConfig.getBedOwnershipConfig().get("BED_RESPAWN_HUNGER_DIVISOR", Integer.class);
            int newFoodLevel = currentFoodLevel / hungerDivisor;
            int minimumHunger = SpecializationConfig.getBedOwnershipConfig().get("BED_RESPAWN_MINIMUM_HUNGER", Integer.class);
            newFoodLevel = Math.max(minimumHunger, newFoodLevel);
            int finalNewFoodLevel = newFoodLevel;
            Bukkit.getScheduler().runTaskLater(Specialization.getInstance(), () -> {
                player.setFoodLevel(finalNewFoodLevel);

                boolean showMessage = SpecializationConfig.getBedOwnershipConfig().get("BED_RESPAWN_SHOW_MESSAGE", Boolean.class);
                if (showMessage) {
                    String message = SpecializationConfig.getBedOwnershipConfig().get("BED_RESPAWN_HUNGER_MESSAGE", String.class);
                    player.sendMessage(message);
                }
            }, 1L); // 1 tick delay
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBedBreak(BlockBreakEvent event) {
        if (!SpecializationConfig.getBedOwnershipConfig().get("BED_OWNERSHIP_ENABLED", Boolean.class)) {
            return;
        }

        Block block = event.getBlock();
        if (!isBed(block.getType())) {
            return;
        }

        // Clear bed ownership from PDC when bed is broken
        Block headBlock = getBedHeadBlock(block);
        if (headBlock == null) {
            return;
        }

        PersistentDataContainer bedPDC = headBlock.getChunk().getPersistentDataContainer();
        String locationKey = getLocationKey(headBlock.getLocation());
        NamespacedKey ownerKey = new NamespacedKey(Specialization.getInstance(), locationKey);

        String ownerUUIDString = bedPDC.get(ownerKey, PersistentDataType.STRING);
        if (ownerUUIDString != null) {
            // Remove ownership data
            bedPDC.remove(ownerKey);

            try {
                Bukkit.getAsyncScheduler().runNow(Specialization.getInstance(), (task) -> {
                    UUID ownerUUID = UUID.fromString(ownerUUIDString);
                    OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerUUID);
                    if (owner != null) {
                        if(owner.isOnline()) {
                            owner.getPlayer().sendMessage("§cYour bed has been destroyed, Spawn Point set to World Spawn!");
                            owner.getPlayer().setBedSpawnLocation(null);
                            owner.getPlayer().setRespawnLocation(null);
                        }
                    }

                    Bukkit.getLogger().info("Bed at " + block.getLocation() + " was broken, cleared PDC ownership for player " + ownerUUID);
                });
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().warning("Invalid UUID found in bed PDC: " + ownerUUIDString);
            }
        }
    }

    /**
     * Clears any previous bed owned by this player
     */
    private void clearPlayerPreviousBed(Player player) {
        // Since we're using PDC, we need to scan nearby chunks for beds owned by this player
        // This is less efficient but eliminates the need for external tracking
        Location playerLoc = player.getLocation();
        int searchRadius = 5; // Search 5 chunks in each direction

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int z = -searchRadius; z <= searchRadius; z++) {
                try {
                    org.bukkit.Chunk chunk = playerLoc.getWorld().getChunkAt(
                        playerLoc.getChunk().getX() + x,
                        playerLoc.getChunk().getZ() + z
                    );

                    PersistentDataContainer chunkPDC = chunk.getPersistentDataContainer();

                    // Check all keys in this chunk for bed ownership by this player
                    for (NamespacedKey key : chunkPDC.getKeys()) {
                        if (key.getNamespace().equals(Specialization.getInstance().getName()) &&
                            key.getKey().startsWith("bed_")) {

                            String ownerUUIDString = chunkPDC.get(key, PersistentDataType.STRING);
                            if (ownerUUIDString != null && ownerUUIDString.equals(player.getUniqueId().toString())) {
                                chunkPDC.remove(key);
                                String unclaimMessage = SpecializationConfig.getBedOwnershipConfig().get("BED_UNCLAIM_MESSAGE", String.class);
                                player.sendMessage(unclaimMessage);
                                return;
                            }
                        }
                    }
                } catch (Exception _) {
                }
            }
        }
    }

    /**
     * Creates a unique key for a bed location
     */
    private String getLocationKey(Location location) {
        return "bed_" + location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ();
    }

    /**
     * Checks if the material is a bed
     */
    private static boolean isBed(Material material) {
        return material.name().endsWith("_BED");
    }

    /**
     * Gets the owner of a specific bed location using PDC
     */
    public static UUID getBedOwner(Location bedLocation) {
        Block headBlock = getBedHeadBlock(bedLocation.getBlock());
        if (headBlock == null) {
            return null;
        }

        PersistentDataContainer bedPDC = headBlock.getChunk().getPersistentDataContainer();
        String locationKey = "bed_" + headBlock.getLocation().getBlockX() + "_" + headBlock.getLocation().getBlockY() + "_" + headBlock.getLocation().getBlockZ();
        String ownerUUIDString = bedPDC.get(new NamespacedKey(Specialization.getInstance(), locationKey), PersistentDataType.STRING);

        if (ownerUUIDString != null) {
            try {
                return UUID.fromString(ownerUUIDString);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Manually set bed ownership using PDC (for admin purposes)
     */
    public static void setBedOwnership(Location bedLocation, UUID playerUUID) {
        Block headBlock = getBedHeadBlock(bedLocation.getBlock());
        if (headBlock == null) {
            return;
        }

        PersistentDataContainer bedPDC = headBlock.getChunk().getPersistentDataContainer();
        String locationKey = "bed_" + headBlock.getLocation().getBlockX() + "_" + headBlock.getLocation().getBlockY() + "_" + headBlock.getLocation().getBlockZ();
        bedPDC.set(new NamespacedKey(Specialization.getInstance(), locationKey), PersistentDataType.STRING, playerUUID.toString());
    }

    /**
     * Gets the head block of a bed, regardless of which part (head or foot) is provided
     */
    private static Block getBedHeadBlock(Block block) {
        if (!isBed(block.getType())) {
            return null;
        }
        
        BlockData blockData = block.getBlockData();
        if (!(blockData instanceof Bed)) {
            return null;
        }
        
        Bed bed = (Bed) blockData;
        
        if (bed.getPart() == Bed.Part.HEAD) {
            // This is already the head block
            return block;
        } else {
            // This is the foot block, get the head block
            // The head is in the direction the bed is facing from the foot
            return block.getRelative(bed.getFacing());
        }
    }
}
