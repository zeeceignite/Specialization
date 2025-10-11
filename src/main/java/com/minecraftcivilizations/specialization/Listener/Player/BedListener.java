package com.minecraftcivilizations.specialization.Listener.Player;

import com.minecraftcivilizations.specialization.Specialization;
import com.minecraftcivilizations.specialization.util.EffectsUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Skull;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;


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
    public void onPlayerInteractWithBed(PlayerBedEnterEvent event) {
        Player player = event.getPlayer();
        Block clickedBlock = event.getBed();

        switch (event.getBedEnterResult()) {
            case OK:
            case NOT_POSSIBLE_NOW:
            case NOT_SAFE:
                break;
            case TOO_FAR_AWAY:
            case OBSTRUCTED:
            case NOT_POSSIBLE_HERE:
            case OTHER_PROBLEM:
                return;
        }

        Block headBlock = getBedHeadBlock(clickedBlock);
        if (headBlock == null) return;

        String bedOwnerUUID = getBedId(headBlock);

        // --- UNCLAIM BED WHEN SHIFTING ---
        if (player.isSneaking() && Objects.equals(bedOwnerUUID, player.getUniqueId().toString())) {
            clearBedId(headBlock);
            clearPlayerBed(player);
            player.setRespawnLocation(null, true);

            float pitch = (float) ThreadLocalRandom.current().nextDouble(0.3, 0.6);
            clickedBlock.getWorld().playSound(clickedBlock.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_PLACE, 10f, pitch);
            player.sendMessage("§6You have unclaimed your bed");
            return;
        }

        // --- CHECK IF BED IS ALREADY CLAIMED ---
        if (bedOwnerUUID != null) {
            if (!bedOwnerUUID.equals(player.getUniqueId().toString())) {
                player.sendMessage("§6This bed is already claimed by another player");
                event.setCancelled(true);
            }
            return; // Already theirs
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
        EffectsUtil.playBlockBoundingBox(player, clickedBlock, Particle.HAPPY_VILLAGER, 0.25 );
        float pitch = (float) ThreadLocalRandom.current().nextDouble(0.9, 1.3);
        clickedBlock.getWorld().playSound(clickedBlock.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 10f, pitch);
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