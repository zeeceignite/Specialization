package com.minecraftcivilizations.specialization.Listener.Player;

import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BedListener implements Listener {

    // Static map to track bed ownership across all players
    private static final Map<Location, UUID> bedOwnership = new HashMap<>();

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

        Location bedLocation = clickedBlock.getLocation();
        UUID currentOwner = bedOwnership.get(bedLocation);

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

        // Cancel the default bed behavior - we'll handle spawn point setting manually
        event.setCancelled(true);

        // Clear player's previous bed if they had one
        if (customPlayer.hasBedLocation()) {
            Location previousBed = customPlayer.getBedLocation();
            if (bedOwnership.get(previousBed) != null && bedOwnership.get(previousBed).equals(player.getUniqueId())) {
                bedOwnership.remove(previousBed);
                String unclaimMessage = SpecializationConfig.getBedOwnershipConfig().get("BED_UNCLAIM_MESSAGE", String.class);
                player.sendMessage(unclaimMessage);
            }
        }

        // Claim the new bed
        bedOwnership.put(bedLocation, player.getUniqueId());
        customPlayer.setBedLocation(bedLocation);

        // Manually set the spawn point using the new API
        player.setRespawnLocation(bedLocation, true);

        String claimMessage = SpecializationConfig.getBedOwnershipConfig().get("BED_CLAIM_MESSAGE", String.class);
        player.sendMessage(claimMessage);

        Bukkit.getLogger().info("Player " + player.getName() + " claimed bed at " + bedLocation + " and set respawn location");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        // Ensure player respawns at their owned bed
        if (!SpecializationConfig.getBedOwnershipConfig().get("BED_OWNERSHIP_ENABLED", Boolean.class)) {
            return;
        }

        Player player = event.getPlayer();
        CustomPlayer customPlayer = CoreUtil.getPlayer(player.getUniqueId());
        if (customPlayer == null || !customPlayer.hasBedLocation()) {
            return;
        }

        Location bedLocation = customPlayer.getBedLocation();
        UUID bedOwner = bedOwnership.get(bedLocation);

        // Verify the player still owns this bed
        if (bedOwner != null && bedOwner.equals(player.getUniqueId())) {
            // Check if bed still exists
            Block bedBlock = bedLocation.getBlock();
            if (isBed(bedBlock.getType())) {
                event.setRespawnLocation(bedLocation);
            } else {
                // Bed was destroyed, clear ownership
                bedOwnership.remove(bedLocation);
                customPlayer.clearBedLocation();
            }
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

        Location bedLocation = block.getLocation();
        UUID owner = bedOwnership.get(bedLocation);

        if (owner != null) {
            // Clear bed ownership
            bedOwnership.remove(bedLocation);

            // Clear from player's CustomPlayer data
            CustomPlayer customPlayer = CoreUtil.getPlayer(owner);
            if (customPlayer != null && customPlayer.isBedLocation(bedLocation)) {
                customPlayer.clearBedLocation();

                // Notify the owner if they're online
                Player ownerPlayer = Bukkit.getPlayer(owner);
                if (ownerPlayer != null) {
                    ownerPlayer.sendMessage("§cYour bed has been destroyed!");
                }
            }

            Bukkit.getLogger().info("Bed at " + bedLocation + " was broken, clearing ownership for player " + owner);
        }
    }

    /**
     * Checks if the material is a bed
     */
    private boolean isBed(Material material) {
        return material.name().endsWith("_BED");
    }

    /**
     * Gets the current bed ownership map (for debugging/admin purposes)
     */
    public static Map<Location, UUID> getBedOwnership() {
        return new HashMap<>(bedOwnership);
    }

    /**
     * Clears all bed ownership (for admin purposes)
     */
    public static void clearAllBedOwnership() {
        bedOwnership.clear();
    }

    /**
     * Gets the owner of a specific bed location
     */
    public static UUID getBedOwner(Location bedLocation) {
        return bedOwnership.get(bedLocation);
    }

    /**
     * Manually set bed ownership (for admin purposes)
     */
    public static void setBedOwnership(Location bedLocation, UUID playerUUID) {
        bedOwnership.put(bedLocation, playerUUID);
    }
}
