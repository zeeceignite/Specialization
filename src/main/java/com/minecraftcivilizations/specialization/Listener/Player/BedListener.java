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

import java.util.Objects;
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
        String bedId = getBedId(bedBlock);
        String playerBedId = getPlayerBedId(player);

        if (bedId != null && !bedId.equals(playerBedId)) {
            event.setCancelled(true);
        }
    }

    // --- CLAIM BED ---
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractWithBed(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();

        if (clickedBlock == null || !isBed(clickedBlock.getType()) || event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!player.getWorld().getEnvironment().equals(World.Environment.NORMAL)) return;

        Block headBlock = getBedHeadBlock(clickedBlock);
        if (headBlock == null) return;

        String bedId = getBedId(headBlock);
        String playerBedId = getPlayerBedId(player);

        if (bedId != null) {
            if (bedId.equals(playerBedId)) {
                player.sendMessage("§6You already claimed this bed.");
            } else {
                player.sendMessage("§cThis bed is already claimed by another player.");
            }
            return;
        }

        // Clear previous bed if any
        clearPlayerBed(player);

        // Assign new ID
        String newId = UUID.randomUUID().toString();
        setBedId(headBlock, newId);

        // Store coordinates + ID in player
        player.getPersistentDataContainer().set(PLAYER_BED_ID, PersistentDataType.STRING, newId);
        player.getPersistentDataContainer().set(PLAYER_BED_X, PersistentDataType.INTEGER, headBlock.getX());
        player.getPersistentDataContainer().set(PLAYER_BED_Y, PersistentDataType.INTEGER, headBlock.getY());
        player.getPersistentDataContainer().set(PLAYER_BED_Z, PersistentDataType.INTEGER, headBlock.getZ());

        player.sendMessage("§aYou have claimed this bed.");
    }

    // --- BED BREAK ---
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBedBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!isBed(block.getType())) return;

        Block headBlock = getBedHeadBlock(block);
        if (headBlock == null) return;

        String bedId = getBedId(headBlock);
        if (bedId != null) {
            clearBedId(headBlock);

            // Clear ID from any player that has it
            for (Player p : Bukkit.getOnlinePlayers()) {
                String playerBedId = getPlayerBedId(p);
                if (bedId.equals(playerBedId)) {
                    clearPlayerBed(p);
                    p.sendMessage("§cYour bed was destroyed.");
                }
            }
        }
    }

    // --- PLAYER RESPAWN ---
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        Integer x = player.getPersistentDataContainer().get(PLAYER_BED_X, PersistentDataType.INTEGER);
        Integer y = player.getPersistentDataContainer().get(PLAYER_BED_Y, PersistentDataType.INTEGER);
        Integer z = player.getPersistentDataContainer().get(PLAYER_BED_Z, PersistentDataType.INTEGER);
        String bedId = player.getPersistentDataContainer().get(PLAYER_BED_ID, PersistentDataType.STRING);

        Bukkit.getLogger().info("[BedDebug] Player " + player.getName() + " respawning. Stored bed ID: " + bedId + " at: " + x + "," + y + "," + z);

        if (x == null || y == null || z == null || bedId == null) {
            player.setRespawnLocation(null);
            Bukkit.getLogger().info("[BedDebug] No saved bed, respawning at world spawn. ");
            return;
        }

        // Force load chunk
        player.getRespawnLocation(true);

        Block bedBlock = player.getWorld().getBlockAt(x, y, z);
        String blockId = getBedId(bedBlock);

        Bukkit.getLogger().info("[BedDebug] Respawn block: " + bedBlock.getType() + " | Bed ID at block: " + blockId);

        if (isBed(bedBlock.getType()) && bedId.equals(blockId)) {
            event.setRespawnLocation(Objects.requireNonNull(getSafeSpawnAbove(bedBlock.getLocation())));
            Bukkit.getLogger().info("[BedDebug] Player " + player.getName() + " respawning at their bed.");
        } else {
            player.setRespawnLocation(null);
            Bukkit.getLogger().info("[BedDebug] Bed missing or mismatched, respawning at world spawn.");
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

    private static void setBedId(Block headBlock, String id) {
        if (!(headBlock.getState() instanceof TileState state)) return;
        state.getPersistentDataContainer().set(BED_OWNER_KEY, PersistentDataType.STRING, id);
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

    private Location getSafeSpawnAbove(Location bedLoc) {
        World world = bedLoc.getWorld();
        if (world == null) return null;

        Location loc = bedLoc.clone().add(0.5, 1, 0.5);

        for (int yOffset = 1; yOffset <= 3; yOffset++) {
            loc.setY(bedLoc.getY() + yOffset);
            if (loc.getBlock().getType().isAir() && loc.clone().add(0, 1, 0).getBlock().getType().isAir()) {
                return loc;
            }
        }

        int highestY = world.getHighestBlockYAt(bedLoc) + 1;
        return new Location(world, bedLoc.getX() + 0.5, highestY, bedLoc.getZ() + 0.5);
    }

    private static World getOverworld() {
        return Bukkit.getWorlds().stream()
                .filter(w -> w.getEnvironment() == World.Environment.NORMAL)
                .findFirst()
                .orElse(Bukkit.getWorlds().getFirst());
    }
}
