package com.minecraftcivilizations.specialization.Reinforcement;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.minecraftcivilizations.specialization.Specialization;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class ReinforcementManager {

    public static final NamespacedKey namespacedKey = new NamespacedKey(Specialization.getInstance(), "reinforcedBlocks");

    private static final Map<Vector, Long> lastTimeSpawnedParticle = new HashMap<>();
    private static final long cooldown = 1000L;

    // store per-chunk index for cycling through reinforced blocks
    private static final Map<Chunk, Integer> chunkIndices = new HashMap<>();

    //This runs a check on all loaded chunks. If the chunk has the namespace it will then
    //Get the locations of the reinforced blocks and spawn 6 particles around the block. Iterating 1 block per check.

    // --------------------- PARTICLE STREAMING ---------------------
    public static void startReinforcement() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (World world : Bukkit.getWorlds()) {
                    for (Chunk chunk : world.getLoadedChunks()) {
                        // Early exit if chunk has no reinforced blocks
                        if (!chunk.getPersistentDataContainer().has(namespacedKey)) continue;

                        Set<Reinforcement> reinforcedSet = getReinforcedBlocks(chunk);
                        if (reinforcedSet == null || reinforcedSet.isEmpty()) continue;

                        // Convert to list for indexed access
                        List<Reinforcement> reinforcedBlocks = new ArrayList<>(reinforcedSet);

                        // Get current index for this chunk
                        int index = chunkIndices.getOrDefault(chunk, 0);
                        if (index >= reinforcedBlocks.size()) index = 0;

                        // Get block to show particle for
                        Reinforcement r = reinforcedBlocks.get(index);

                        // Show particle to all players currently seeing this chunk
                        for (Player player : chunk.getPlayersSeeingChunk()) {
                            spawnParticle(player, r);
                        }

                        // Update index for next tick
                        chunkIndices.put(chunk, (index + 1) % reinforcedBlocks.size());
                    }
                }
            }
        }.runTaskTimer(Specialization.getInstance(), 0L, 1L); // every tick
    }

    private static void spawnParticle(Player player, Reinforcement r) {
        long now = System.currentTimeMillis();
        Long last = lastTimeSpawnedParticle.get(r.location());
        if (last != null && now - last < cooldown) return;

        Block b = player.getWorld().getBlockAt(r.location().getBlockX(), r.location().getBlockY(), r.location().getBlockZ());
        Location base = b.getLocation().add(0.5, 0.5, 0.5);

        double offset = 0.55;
        Vector[] directions = new Vector[]{
                new Vector(offset, 0, 0),
                new Vector(-offset, 0, 0),
                new Vector(0, offset, 0),
                new Vector(0, -offset, 0),
                new Vector(0, 0, offset),
                new Vector(0, 0, -offset)
        };

        for (Vector v : directions) {
            Location loc = base.clone().add(v)
                    .add(Math.random() * 0.1 - 0.05, Math.random() * 0.1 - 0.05, Math.random() * 0.1 - 0.05);
            player.spawnParticle(Particle.CRIT, loc, 1, 0, 0, 0, 0);
        }

        lastTimeSpawnedParticle.put(r.location(), now);
    }

    // --------------------- REINFORCEMENT METHODS ---------------------
    public static boolean addReinforcement(Player player, Block block, boolean isHeavy) {
        if (isHeavy ? isHeavilyReinforced(block) : (isLightlyReinforced(block) || isHeavilyReinforced(block)))
            return false;

        Chunk chunk = block.getChunk();
        Set<Reinforcement> blocks = getReinforcedBlocks(chunk);
        if (blocks == null) blocks = new HashSet<>();
        if (!blocks.add(new Reinforcement(block.getLocation().toVector(), isHeavy))) return false;

        chunk.getPersistentDataContainer().set(namespacedKey, PersistentDataType.STRING, new Gson().toJson(blocks));

        Player target = player != null ? player : block.getWorld().getNearbyPlayers(block.getLocation(), 4.0)
                .stream().findFirst().orElse(null);
        if (target != null) {
            CustomPlayer cp = CoreUtil.getPlayer(target.getUniqueId());
            if (cp != null) cp.addSkillXp(SkillType.BUILDER, isHeavy ? 15.0 : 5.0);
        }
        return true;
    }

    public static boolean addReinforcement(Block block, boolean isHeavy) {
        return addReinforcement(null, block, isHeavy);
    }

    public static boolean addReinforcementSilent(Block b, boolean h) {
        if (h ? isHeavilyReinforced(b) : (isLightlyReinforced(b) || isHeavilyReinforced(b))) return false;
        Chunk c = b.getChunk();
        Set<Reinforcement> r = getReinforcedBlocks(c);
        if (r == null) r = new HashSet<>();
        if (!r.add(new Reinforcement(b.getLocation().toVector(), h))) return false;
        c.getPersistentDataContainer().set(namespacedKey, PersistentDataType.STRING, new Gson().toJson(r));
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
        return getReinforcement(block) != null;
    }

    public static boolean isHeavilyReinforced(Block block) {
        Reinforcement r = getReinforcement(block);
        return r != null && r.isHeavy();
    }

    public static boolean isLightlyReinforced(Block block) {
        Reinforcement r = getReinforcement(block);
        return r != null && !r.isHeavy();
    }

    private static Reinforcement getReinforcement(Block block) {
        Set<Reinforcement> blocks = getReinforcedBlocks(block.getChunk());
        if (blocks == null) return null;
        for (Reinforcement r : blocks) {
            if (r.location().getBlockX() == block.getX() &&
                    r.location().getBlockY() == block.getY() &&
                    r.location().getBlockZ() == block.getZ()) {
                return r;
            }
        }
        return null;
    }

    private static Set<Reinforcement> getReinforcedBlocks(Chunk chunk) {
        if (!chunk.getPersistentDataContainer().has(namespacedKey)) return null;
        String s = chunk.getPersistentDataContainer().get(namespacedKey, PersistentDataType.STRING);
        return new Gson().fromJson(s, new TypeToken<Set<Reinforcement>>() {
        }.getType());
    }
}
