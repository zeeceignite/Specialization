package com.minecraftcivilizations.specialization.Listener;

import com.google.gson.Gson;
import com.minecraftcivilizations.specialization.Config.Config;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Recipe.Pair;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import minecraftcivilizations.com.minecraftCivilizationsCore.MinecraftCivilizationsCore;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class PlaceBlockListener implements Listener {
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Pair pair = new Gson().fromJson(Config.getXpGainFromPlacingConfig().getString(event.getBlockPlaced().getType().name()), Pair.class);
        if (pair != null) {
            CustomPlayer customPlayer = (CustomPlayer) MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().getCustomPlayer(event.getPlayer().getUniqueId());
            customPlayer.addSkillXp(SkillType.valueOf(pair.key()), Double.parseDouble(pair.value()));
        }
    }
}
