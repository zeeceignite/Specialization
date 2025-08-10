package com.minecraftcivilizations.specialization.Listener;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Reinforcement.ReinforcementManager;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.Specialization;
import minecraftcivilizations.com.minecraftCivilizationsCore.MinecraftCivilizationsCore;
import minecraftcivilizations.com.minecraftCivilizationsCore.Options.Pair;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
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
            CustomPlayer customPlayer = (CustomPlayer) MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().getCustomPlayer(event.getPlayer().getUniqueId());
            customPlayer.addSkillXp(pair.firstValue(), pair.secondValue());
        }
    }

}
