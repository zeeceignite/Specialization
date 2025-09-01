package com.minecraftcivilizations.specialization.Listener.Mobs;

import com.google.gson.reflect.TypeToken;
import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.Skill;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Comparator;
import java.util.List;

public class MobKillListener implements Listener {

    @EventHandler
    public void GuardsmanKillListener(EntityDeathEvent e){
        if (e.getEntity().getKiller() != null) {
            Player player = e.getEntity().getKiller();
            assert player != null;
            CustomPlayer killer = CoreUtil.getPlayer(e.getEntity().getKiller().getUniqueId());

            EntityType entity = e.getEntity().getType();

            Double xp = SpecializationConfig.getGuardsmanConfig().get(entity, Double.class);
            killer.addSkillXp(SkillType.GUARDSMAN, xp);

            addCustomDrops(e);
        }
    }

    public void addCustomDrops(EntityDeathEvent e){
        List<NamespacedKey> items = SpecializationConfig.getMobDropsConfig().get(e.getEntityType(), new TypeToken<>() {});
        Material.matchMaterial(e.getEntityType().getKey().getKey());

    }

    @EventHandler
    public void GuardsmanDamageListener (EntityDamageByEntityEvent event) {
        // Check if the damager is a player
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        
        Player damager = (Player) event.getDamager();
        CustomPlayer customPlayer = CoreUtil.getPlayer(damager);
        
        // Get the player's highest skill (main class)
        Skill bestSkill = customPlayer.getSkills().stream()
                .max(Comparator.comparingDouble(Skill::getXp))
                .orElse(null);
        
        // If player has no skills or their main class is Guardsman, no damage reduction
        if (bestSkill == null || bestSkill.getSkillType() == SkillType.GUARDSMAN) {
            return;
        }
        
        // Apply damage reduction for non-Guardsman players attacking mobs
        double damageReduction = SpecializationConfig.getGuardsmanConfig().get("NON_GUARDSMAN_DAMAGE_REDUCTION", Double.class);
        double currentDamage = event.getFinalDamage();
        double reducedDamage = currentDamage * (1.0 - damageReduction);
        
        event.setDamage(reducedDamage);
    }
}
