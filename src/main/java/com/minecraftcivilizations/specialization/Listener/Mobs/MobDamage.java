package com.minecraftcivilizations.specialization.Listener.Mobs;

import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Listener.Player.Combat.CombatManager;
import com.minecraftcivilizations.specialization.MobGoals.BreakBlockMobGoal;
import com.minecraftcivilizations.specialization.MobGoals.TargetPlayerMobGoal;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.Specialization;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntitySpawnEvent;

public class MobDamage implements Listener {

    CombatManager combatManager;

    public MobDamage(CombatManager combatManager) {
        this.combatManager = combatManager;
        this.combatManager.getPlugin().getServer().getPluginManager().registerEvents(this, combatManager.getPlugin());
    }

    @EventHandler
    public void onMobSpawn(EntitySpawnEvent event) {

        if (event.getEntity() instanceof Monster monster) {

            AttributeInstance attribute = monster.getAttribute(Attribute.MAX_HEALTH);
            double max_health = attribute.getBaseValue()*2;
            attribute.setBaseValue(max_health);
            monster.setHealth(max_health);
//            monster.registerAttribute(Attribute.MAX_HEALTH);
//            AttributeInstance attribute = monster.getAttribute(Attribute.MAX_HEALTH);
//            AttributeModifier modifier = attribute.getModifier(new NamespacedKey(Specialization.getInstance(), "max_health"));
//            attribute.addModifier(modifier);

//                double new_value = attr.getBaseValue() * 2.0;
//                attr.setBaseValue(new_value);
//                attr.addModifier(AttributeModifier);
        }
        if(event.getEntity() instanceof LivingEntity){
            return; //temporary logic disable
        }

        if(!(event.getEntity() instanceof Monster monster)) return;
        double speedAddition;

        if(monster.getWorld().isDayTime()) speedAddition = SpecializationConfig.getMobConfig().get("DAYTIME_SPEED_BUFF", Double.class);
        else speedAddition = SpecializationConfig.getMobConfig().get("NIGHTTIME_SPEED_BUFF", Double.class);

        AttributeInstance attribute = monster.getAttribute(Attribute.MOVEMENT_SPEED);
        if(attribute == null) {
            return;
        }
        attribute.setBaseValue(attribute.getValue() + speedAddition);

        Bukkit.getMobGoals().addGoal(monster,3, new BreakBlockMobGoal(monster));
        Bukkit.getMobGoals().addGoal(monster,0, new TargetPlayerMobGoal(monster));
    }

    /**
     * Amplifies mob damage
     */
    public void onMobAttack(Player player, EntityDamageByEntityEvent event){
        if(!(event.getDamager() instanceof Enemy enemy)) return;

//        double newDamage = event.getDamage(EntityDamageEvent.DamageModifier.BASE);
//        event.setDamage(EntityDamageEvent.DamageModifier.BASE, newDamage*2.0);

//      BACKUP PLAN FOR MOB DAMAGE:
//        Ensure this method is called after CombatManager's ArmorReduction.
//        double newDamage = event.getDamage();
//        event.setDamage(newDamage*2.0);



//        event.setDamage(EntityDamageEvent.DamageModifier.BASE, newDamage*2);
//        if(player.getWorld().isDayTime()){
//            double dayMobDamageMultiplier = SpecializationConfig.getMobConfig().get("DAYTIME_MOB_DAMAGE_MULTIPLIER", Double.class);
//            newDamage = event.getDamage() * dayMobDamageMultiplier;
//        }else{
//            double nightMobDamageMultiplier = SpecializationConfig.getMobConfig().get("NIGHTTIME_MOB_DAMAGE_MULTIPLIER", Double.class);
//            newDamage = event.getDamage() * nightMobDamageMultiplier;
//            if(CoreUtil.getPlayer(player).getSkillLevel(SkillType.GUARDSMAN) >= 1){
//                double guardsmanReductionAmount = SpecializationConfig.getMobConfig().get("NIGHT_GUARDSMAN_MOB_DAMAGE_PERCENT_REDUCTION", Double.class);
//                newDamage *= 1 - (guardsmanReductionAmount/100d);
//            }
//        }
//        event.setDamage(newDamage);
    }

}
