package com.minecraftcivilizations.specialization.Reinforcement;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.minecraftcivilizations.specialization.Specialization;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class ReinforcementManager {

    public static final NamespacedKey namespacedKey = new NamespacedKey(Specialization.getInstance(), "reinforcedBlocks");
    public static final Map<Vector, Long> lastTimeSpawnedParticle = new HashMap<>(0);
    public static final Long cooldown = 100L;

    public static void checkForReinforcements(Player player) {
        Set<Chunk> chunksNearPlayer = getChunksNearPlayer(player, 3);
        for (Chunk chunk : chunksNearPlayer) {
            Set<Reinforcement> reinforcedBlocks = getReinforcedBlocks(chunk);
            if (reinforcedBlocks != null) {
                for (Reinforcement vector : reinforcedBlocks) {
                    if (lastTimeSpawnedParticle.containsKey(vector.location()) && System.currentTimeMillis() - lastTimeSpawnedParticle.get(vector.location()) > cooldown) {
                        Block blockAt = player.getWorld().getBlockAt(vector.location().getBlockX(), vector.location().getBlockY(), vector.location().getBlockZ());
                        player.spawnParticle(Particle.CRIT, blockAt.getLocation().toBlockLocation().add(.5, 0.5, .5), 200, .25, .25, .25, 0);
                        lastTimeSpawnedParticle.put(vector.location(), System.currentTimeMillis());
                    } else if (!lastTimeSpawnedParticle.containsKey(vector.location())) {
                        lastTimeSpawnedParticle.put(vector.location(), System.currentTimeMillis());
                    }
                }
            }
        }
    }

    public static boolean addReinforcement(Block block, boolean isHeavy) {
        Chunk chunk = block.getChunk();

        if (isHeavy && isHeavilyReinforced(block)) return false;
        if (!isHeavy && isLightlyReinforced(block)) return false;
        if (!isHeavy && isHeavilyReinforced(block)) return false;
        Set<Reinforcement> reinforcedBlocks = getReinforcedBlocks(chunk);
        if (reinforcedBlocks == null) {
            reinforcedBlocks = new HashSet<>();
        }
        reinforcedBlocks.add(new Reinforcement(block.getLocation().toVector(), isHeavy));
        chunk.getPersistentDataContainer().set(namespacedKey, PersistentDataType.STRING, new Gson().toJson(reinforcedBlocks));
        return true;
    }

    public static void removeReinforcement(Block block) {
        Chunk chunk = block.getChunk();
        Set<Reinforcement> reinforcedBlocks = getReinforcedBlocks(chunk);
        if (reinforcedBlocks == null) return;
        reinforcedBlocks.remove(new Reinforcement(block.getLocation().toVector(), false));
        reinforcedBlocks.remove(new Reinforcement(block.getLocation().toVector(), true));
        chunk.getPersistentDataContainer().set(namespacedKey, PersistentDataType.STRING, new Gson().toJson(reinforcedBlocks));
    }

    public static boolean isReinforced(Block block) {
        Chunk chunk = block.getChunk();
        Set<Reinforcement> reinforcedBlocks = getReinforcedBlocks(chunk);
        if (reinforcedBlocks != null) {
            for (Reinforcement vector : reinforcedBlocks) {
                if (vector.location().getX() == block.getX() && vector.location().getY() == block.getY() && vector.location().getZ() == block.getZ()) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isHeavilyReinforced(Block block) {
        Chunk chunk = block.getChunk();
        Set<Reinforcement> reinforcedBlocks = getReinforcedBlocks(chunk);
        if (reinforcedBlocks != null) {
            for (Reinforcement vector : reinforcedBlocks) {
                if (vector.location().getX() == block.getX() && vector.location().getY() == block.getY() && vector.location().getZ() == block.getZ() && vector.isHeavy()) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isLightlyReinforced(Block block) {
        Chunk chunk = block.getChunk();
        Set<Reinforcement> reinforcedBlocks = getReinforcedBlocks(chunk);
        if (reinforcedBlocks != null) {
            for (Reinforcement vector : reinforcedBlocks) {
                if (vector.location().getX() == block.getX() && vector.location().getY() == block.getY() && vector.location().getZ() == block.getZ() && !vector.isHeavy()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Set<Reinforcement> getReinforcedBlocks(Chunk chunk) {
        if (chunk.getPersistentDataContainer().has(namespacedKey)) {
            String s = chunk.getPersistentDataContainer().get(namespacedKey, PersistentDataType.STRING);
            Set<Reinforcement> list = new Gson().fromJson(s, new TypeToken<Set<Reinforcement>>() {}.getType());
            if (list == null) return null;
            return list;
        }
        return null;
    }

    private static Set<Chunk> getChunksNearPlayer(Player player, int radius) {
        Set<Chunk> nearbyChunks = new HashSet<>();

        int centerX = player.getLocation().getChunk().getX();
        int centerZ = player.getLocation().getChunk().getZ();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                Chunk chunk = player.getWorld().getChunkAt(centerX + dx, centerZ + dz);
                nearbyChunks.add(chunk);
            }
        }

        return nearbyChunks;
    }


    public static void startReinforcement() {
        new BukkitRunnable() {

            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    ReinforcementManager.checkForReinforcements(player);
                }
            }
        }.runTaskTimerAsynchronously(Specialization.getInstance(), 0, 20);

    }

}
