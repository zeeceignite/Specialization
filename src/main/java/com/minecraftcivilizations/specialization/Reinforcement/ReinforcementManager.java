package com.minecraftcivilizations.specialization.Reinforcement;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.minecraftcivilizations.specialization.Specialization;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReinforcementManager {

    public static final NamespacedKey namespacedKey = new NamespacedKey(Specialization.getInstance(), "reinforcedBlocks");
    public static final Map<Vector, Long> lastTimeSpawnedParticle = new java.util.HashMap<>(0);
    public static final Long cooldown = 100L;

    public static void checkForReinforcements(Player player) {
        Set<Chunk> chunksNearPlayer = getChunksNearPlayer(player, 3);
        for (Chunk chunk : chunksNearPlayer) {
            Set<Vector> reinforcedBlocks = getReinforcedBlocks(chunk);
            if (reinforcedBlocks != null) {
                for (Vector vector : reinforcedBlocks) {
                    Specialization.logger.info("Vector " + vector.getX() + " " + vector.getY() + " " + vector.getZ());
                    if (lastTimeSpawnedParticle.containsKey(vector) && System.currentTimeMillis() - lastTimeSpawnedParticle.get(vector) > cooldown) {
                        Block blockAt = player.getWorld().getBlockAt(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
                        player.getWorld().spawnParticle(Particle.CRIT, blockAt.getLocation().toBlockLocation(), 200, .5, .5, .5);
                        lastTimeSpawnedParticle.put(vector, System.currentTimeMillis());
                    } else if (!lastTimeSpawnedParticle.containsKey(vector)) {
                        lastTimeSpawnedParticle.put(vector, System.currentTimeMillis());
                    }
                }
            }
        }
    }

    public static boolean isReinforced(Block block) {
        Chunk chunk = block.getChunk();
        Set<Vector> reinforcedBlocks = getReinforcedBlocks(chunk);
        if (reinforcedBlocks != null) {
            for (Vector vector : reinforcedBlocks) {
                if (vector.getX() == block.getX() && vector.getY() == block.getY() && vector.getZ() == block.getZ()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Set<Vector> getReinforcedBlocks(Chunk chunk) {
        if (chunk.getPersistentDataContainer().has(namespacedKey)) {
            String s = chunk.getPersistentDataContainer().get(namespacedKey, PersistentDataType.STRING);
            Set<Vector> list = new Gson().fromJson(s, new TypeToken<Set<Vector>>() {}.getType());
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

}
