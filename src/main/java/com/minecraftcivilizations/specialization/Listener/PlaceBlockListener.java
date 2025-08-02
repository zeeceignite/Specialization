package com.minecraftcivilizations.specialization.Listener;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.Specialization;
import minecraftcivilizations.com.minecraftCivilizationsCore.MinecraftCivilizationsCore;
import minecraftcivilizations.com.minecraftCivilizationsCore.Options.Pair;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlaceBlockListener implements Listener {

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Pair<SkillType, Double> pair = SpecializationConfig.getXpGainFromPlacingConfig().get(event.getBlockPlaced().getType(), new TypeToken<>() {});
        if (pair != null) {

            NamespacedKey namespacedKey = new NamespacedKey(Specialization.getInstance(), "reinforcedBlocks");

            CustomPlayer customPlayer = (CustomPlayer) MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().getCustomPlayer(event.getPlayer().getUniqueId());
            Chunk chunk = event.getBlockPlaced().getChunk();
            if (chunk.getPersistentDataContainer().has(namespacedKey)) {
                String s = chunk.getPersistentDataContainer().get(namespacedKey, PersistentDataType.STRING);
                Set<Vector> list = new Gson().fromJson(s, new TypeToken<Set<Vector>>() {}.getType());
                if (list == null) list = new HashSet<>();
                list.add(new Vector(event.getBlockPlaced().getX(), event.getBlockPlaced().getY(), event.getBlockPlaced().getZ()));
                chunk.getPersistentDataContainer().set(namespacedKey, PersistentDataType.STRING, new Gson().toJson(list, new TypeToken<Set<Vector>>() {}.getType()));
            } else {
                Set<Vector> list = new HashSet<>();
                list.add(new Vector(event.getBlockPlaced().getX(), event.getBlockPlaced().getY(), event.getBlockPlaced().getZ()));
                chunk.getPersistentDataContainer().set(namespacedKey, PersistentDataType.STRING, new Gson().toJson(list, new TypeToken<Set<Vector>>() {}.getType()));
            }

            customPlayer.addSkillXp(pair.firstValue(), pair.secondValue());
        }
    }

}
