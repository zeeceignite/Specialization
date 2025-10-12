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
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import com.minecraftcivilizations.specialization.Config.SpecializationConfig;

import java.util.*;

public class ReinforcementManager {

    public static final NamespacedKey namespacedKey = new NamespacedKey(Specialization.getInstance(), "reinforcedBlocks");
    public static final Map<Vector, Long> lastTimeSpawnedParticle = new HashMap<>(0);
    public static final Long cooldown = 100L;

    public static void checkForReinforcements(Player player) {
        for (Chunk chunk : getChunksNearPlayer(player, 3)) {
            Set<Reinforcement> blocks = getReinforcedBlocks(chunk);
            if (blocks == null) continue;
            for (Reinforcement r : blocks) {
                long now = System.currentTimeMillis();
                Long last = lastTimeSpawnedParticle.get(r.location());
                if (last == null || now - last > cooldown) {
                    Block b = player.getWorld().getBlockAt(r.location().getBlockX(), r.location().getBlockY(), r.location().getBlockZ());
                    player.spawnParticle(Particle.CRIT, b.getLocation().toBlockLocation().add(.5, 0.5, .5), 200, .25, .25, .25, 0);
                    lastTimeSpawnedParticle.put(r.location(), now);
                }
            }
        }
    }

    public static boolean addReinforcement(Player player, Block block, boolean isHeavy) {
        if (isHeavy ? isHeavilyReinforced(block) : (isLightlyReinforced(block) || isHeavilyReinforced(block))) return false;
        
        Chunk chunk = block.getChunk();
        Set<Reinforcement> blocks = getReinforcedBlocks(chunk);
        if (blocks == null) blocks = new HashSet<>();
        if (!blocks.add(new Reinforcement(block.getLocation().toVector(), isHeavy))) return false;
        
        chunk.getPersistentDataContainer().set(namespacedKey, PersistentDataType.STRING, new Gson().toJson(blocks));
        
        Player target = player != null ? player : block.getWorld().getNearbyPlayers(block.getLocation(), 4.0).stream().findFirst().orElse(null);
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
            if (r.location().getX() == block.getX() && r.location().getY() == block.getY() && r.location().getZ() == block.getZ()) {
                return r;
            }
        }
        return null;
    }

    private static Set<Reinforcement> getReinforcedBlocks(Chunk chunk) {
        if (!chunk.getPersistentDataContainer().has(namespacedKey)) return null;
        String s = chunk.getPersistentDataContainer().get(namespacedKey, PersistentDataType.STRING);
        return new Gson().fromJson(s, new TypeToken<Set<Reinforcement>>() {}.getType());
    }

    private static Set<Chunk> getChunksNearPlayer(Player player, int radius) {
        Set<Chunk> chunks = new HashSet<>();
        Chunk center = player.getLocation().getChunk();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                chunks.add(player.getWorld().getChunkAt(center.getX() + dx, center.getZ() + dz));
            }
        }
        return chunks;
    }


    public static void startReinforcement() {
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.getOnlinePlayers().forEach(ReinforcementManager::checkForReinforcements);
            }
        }.runTaskTimerAsynchronously(Specialization.getInstance(), 0, 20);
    }

}
