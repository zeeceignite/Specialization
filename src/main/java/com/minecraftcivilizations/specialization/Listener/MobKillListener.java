package com.minecraftcivilizations.specialization.Listener;

import com.google.gson.reflect.TypeToken;
import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import minecraftcivilizations.com.minecraftCivilizationsCore.Options.Pair;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class MobKillListener implements Listener {

    @EventHandler
    public void GuardsmanKillListener(EntityDeathEvent e){
        Player player = e.getEntity().getKiller();
        assert player != null;
        CustomPlayer killer = CoreUtil.getPlayer(e.getEntity().getKiller().getUniqueId());

        EntityType entity = e.getEntity().getType();

        Double xp = SpecializationConfig.getGuardsmanConfig().get(entity, Double.class);
        killer.addSkillXp(SkillType.GUARDSMAN, xp);
    }
}
