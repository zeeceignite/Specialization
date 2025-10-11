package com.minecraftcivilizations.specialization.Listener.Player;

import com.minecraftcivilizations.specialization.Specialization;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.persistence.PersistentDataType;
import java.util.UUID;

public class BedListener implements Listener {

    private static final NamespacedKey BED_OWNER_KEY = new NamespacedKey(Specialization.getInstance(), "bed_owner");
    private static final NamespacedKey PLAYER_BED_ID = new NamespacedKey(Specialization.getInstance(), "bed_id");
    private static final NamespacedKey PLAYER_BED_X = new NamespacedKey(Specialization.getInstance(), "bed_x");
    private static final NamespacedKey PLAYER_BED_Y = new NamespacedKey(Specialization.getInstance(), "bed_y");
    private static final NamespacedKey PLAYER_BED_Z = new NamespacedKey(Specialization.getInstance(), "bed_z");

    // --- BED ENTER ---
    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        Player player = event.getPlayer();
        Block bedBlock = event.getBed();
        String bedOwnerUUID = getBedId(bedBlock);
        if (bedOwnerUUID == null) return;

        if (!bedOwnerUUID.equals(player.getUniqueId().toString())) {
            event.setCancelled(true);
        }
    }

    // --- CLAIM BED ---
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractWithBed(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || !isBed(clickedBlock.getType()) || event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        World world = player.getWorld();
        if (!world.isBedWorks()) return;  // Bed can explode here
        if (!world.isNatural()) return;   // Cannot set spawn

        // Vanilla distance check
        Location bedLoc = clickedBlock.getLocation();
        Location playerLoc = player.getLocation();
        double dx = Math.abs(playerLoc.getX() - (bedLoc.getX() + 0.5));
        double dy = Math.abs(playerLoc.getY() - bedLoc.getY());
        double dz = Math.abs(playerLoc.getZ() - (bedLoc.getZ() + 0.5));
        if (dx > 3.0 || dy > 2.0 || dz > 3.0) {
            return;
        }

        Block headBlock = getBedHeadBlock(clickedBlock);
        if (headBlock == null) return;

        String bedOwnerUUID = getBedId(headBlock);

        // If already claimed
        if (bedOwnerUUID != null) {
            if (bedOwnerUUID.equals(player.getUniqueId().toString())) {
                return;
            } else {
                player.sendMessage("§cThis bed is already claimed by another player");
                event.setCancelled(true);
            }
            return;
        }

        // Clear old bed PDC if player had a previous bed
        clearOldBed(player);

        // Claim this bed
        String uuidStr = player.getUniqueId().toString();
        setBedId(headBlock, uuidStr);
        player.getPersistentDataContainer().set(PLAYER_BED_ID, PersistentDataType.STRING, uuidStr);
        player.getPersistentDataContainer().set(PLAYER_BED_X, PersistentDataType.INTEGER, headBlock.getX());
        player.getPersistentDataContainer().set(PLAYER_BED_Y, PersistentDataType.INTEGER, headBlock.getY());
        player.getPersistentDataContainer().set(PLAYER_BED_Z, PersistentDataType.INTEGER, headBlock.getZ());
        player.sendMessage("§aYou have claimed this bed");
    }

    // --- BED BREAK ---
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBedBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!isBed(block.getType())) return;

        Block headBlock = getBedHeadBlock(block);
        if (headBlock == null) return;

        String bedOwnerUUIDStr = getBedId(headBlock);
        if (bedOwnerUUIDStr != null) {
            clearBedId(headBlock);

            // Directly get player by UUID
            UUID ownerUUID = UUID.fromString(bedOwnerUUIDStr);
            Player owner = Bukkit.getPlayer(ownerUUID);
            if (owner != null) {
                clearPlayerBed(owner);
                owner.setRespawnLocation(null, true);
                owner.sendMessage("§cYour bed was destroyed");
            }
        }
    }

    // --- PLAYER RESPAWN ---
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        String bedId = player.getPersistentDataContainer().get(PLAYER_BED_ID, PersistentDataType.STRING);

        if (bedId == null) return; // vanilla handles spawn naturally

        Integer x = player.getPersistentDataContainer().get(PLAYER_BED_X, PersistentDataType.INTEGER);
        Integer y = player.getPersistentDataContainer().get(PLAYER_BED_Y, PersistentDataType.INTEGER);
        Integer z = player.getPersistentDataContainer().get(PLAYER_BED_Z, PersistentDataType.INTEGER);

        // If the bed no longer exists, clear the player data
        if (x == null || y == null || z == null) {
            clearPlayerBed(player);
            return;
        }

        Block bedBlock = player.getWorld().getBlockAt(x, y, z);
        Block headBlock = getBedHeadBlock(bedBlock);

        if (headBlock == null || !bedId.equals(getBedId(headBlock))) {
            clearPlayerBed(player);
        }
    }


    // --- HELPERS ---
    private static boolean isBed(Material material) {
        return material.name().endsWith("_BED");
    }

    private static Block getBedHeadBlock(Block block) {
        if (!isBed(block.getType())) return null;
        BlockData data = block.getBlockData();
        if (!(data instanceof Bed bed)) return null;
        return bed.getPart() == Bed.Part.HEAD ? block : block.getRelative(bed.getFacing());
    }

    private static String getBedId(Block headBlock) {
        if (!(headBlock.getState() instanceof TileState state)) return null;
        return state.getPersistentDataContainer().get(BED_OWNER_KEY, PersistentDataType.STRING);
    }

    private static void setBedId(Block headBlock, String uuid) {
        if (!(headBlock.getState() instanceof TileState state)) return;
        state.getPersistentDataContainer().set(BED_OWNER_KEY, PersistentDataType.STRING, uuid);
        state.update(true);
    }

    private static void clearBedId(Block headBlock) {
        if (!(headBlock.getState() instanceof TileState state)) return;
        state.getPersistentDataContainer().remove(BED_OWNER_KEY);
        state.update(true);
    }

    private static String getPlayerBedId(Player player) {
        return player.getPersistentDataContainer().get(PLAYER_BED_ID, PersistentDataType.STRING);
    }

    private static void clearPlayerBed(Player player) {
        player.getPersistentDataContainer().remove(PLAYER_BED_ID);
        player.getPersistentDataContainer().remove(PLAYER_BED_X);
        player.getPersistentDataContainer().remove(PLAYER_BED_Y);
        player.getPersistentDataContainer().remove(PLAYER_BED_Z);
    }

    private void clearOldBed(Player player) {
        Integer oldX = player.getPersistentDataContainer().get(PLAYER_BED_X, PersistentDataType.INTEGER);
        Integer oldY = player.getPersistentDataContainer().get(PLAYER_BED_Y, PersistentDataType.INTEGER);
        Integer oldZ = player.getPersistentDataContainer().get(PLAYER_BED_Z, PersistentDataType.INTEGER);

        if (oldX != null && oldY != null && oldZ != null) {
            player.getWorld().getChunkAt(oldX, oldZ).load(true);
            Block oldBedBlock = player.getWorld().getBlockAt(oldX, oldY, oldZ);
            Block oldHead = getBedHeadBlock(oldBedBlock);
            if (oldHead != null) clearBedId(oldHead);
        }
        clearPlayerBed(player);
    }
}