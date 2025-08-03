package com.minecraftcivilizations.specialization.Listener;

import com.google.gson.reflect.TypeToken;
import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.Skill;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.List;

public class PlayerInteractListener implements Listener {

    @EventHandler
    public void onRightClickBlock(PlayerInteractEvent e) {
        if(e.getAction().isLeftClick() || e.getClickedBlock() == null) return;
        List<Material> defaultAllow = SpecializationConfig.getCanUseBlockConfig().get("default", new TypeToken<>(){});
        Material clickedType = e.getClickedBlock().getType();
        if(defaultAllow.contains(clickedType)) {
            e.setCancelled(true);
            return;
        }

        CustomPlayer player = CoreUtil.getPlayer(e.getPlayer());
        for (Skill skill : player.getSkills()) {
            List<Material> blocks = SpecializationConfig.getCanUseBlockConfig().get(skill.getSkillType()+"_"+player.getSkillLevelEnum(skill.getSkillType()), new TypeToken<>(){});
            if(blocks.contains(clickedType)) {
                e.setCancelled(true);
                return;
            }
        }
    }

}
