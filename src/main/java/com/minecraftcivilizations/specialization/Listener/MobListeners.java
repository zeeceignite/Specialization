package com.minecraftcivilizations.specialization.Listener;

import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.Specialization;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntitySpawnEvent;

public class MobListeners implements Listener {

    @EventHandler
    public void onMobSpawn(EntitySpawnEvent event) {
        if(!(event.getEntity() instanceof Enemy monster)) return;
        double speedAddition;

        if(monster.getWorld().isDayTime()) speedAddition = SpecializationConfig.getMobConfig().get("DAYTIME_SPEED_BUFF", Double.class);
        else speedAddition = SpecializationConfig.getMobConfig().get("NIGHTTIME_SPEED_BUFF", Double.class);

        AttributeInstance attribute = monster.getAttribute(Attribute.MOVEMENT_SPEED);
        if(attribute == null) {
            Specialization.logger.warning("Mob spawned and didn't have movement speed:" + monster.getType());
            return;
        }

        attribute.setBaseValue(attribute.getDefaultValue() + speedAddition);
    }

    @EventHandler
    public void onMobAttack(EntityDamageByEntityEvent event){
        if(!(event.getDamager() instanceof Enemy)) return;
        if(!(event.getEntity() instanceof Player player)) return;

        double newDamage;
        if(player.getWorld().isDayTime()){
            double dayMobDamageMultiplier = SpecializationConfig.getMobConfig().get("DAYTIME_MOB_DAMAGE_MULTIPLIER", Double.class);
            newDamage = event.getDamage() * dayMobDamageMultiplier;
        }else{
            double nightMobDamageMultiplier = SpecializationConfig.getMobConfig().get("NIGHTTIME_MOB_DAMAGE_MULTIPLIER", Double.class);
            newDamage = event.getDamage() * nightMobDamageMultiplier;
            if(CoreUtil.getPlayer(player).getSkillLevel(SkillType.GUARDSMAN) > 2){
                double guardsmanReductionAmount = SpecializationConfig.getMobConfig().get("NIGHT_GUARDSMAN_MOB_DAMAGE_PERCENT_REDUCTION", Double.class);
                newDamage *= 1 - (guardsmanReductionAmount/100d);
            }
        }
        event.setDamage(newDamage);
    }

}
