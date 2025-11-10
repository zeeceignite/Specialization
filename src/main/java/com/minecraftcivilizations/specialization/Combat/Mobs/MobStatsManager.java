package com.minecraftcivilizations.specialization.Combat.Mobs;

import com.minecraftcivilizations.specialization.Combat.CombatManager;
import com.minecraftcivilizations.specialization.MobGoals.BreakBlockMobGoal;
import com.minecraftcivilizations.specialization.MobGoals.TargetPlayerMobGoal;
import com.minecraftcivilizations.specialization.StaffTools.Debug;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntitySpawnEvent;

import java.util.EnumMap;

import static org.bukkit.event.entity.EntityDamageEvent.DamageModifier.*;
import static org.bukkit.entity.EntityType.*;

public class MobStatsManager implements Listener {

    CombatManager combatManager;

    public MobStatsManager(CombatManager combatManager) {
        this.combatManager = combatManager;
        this.combatManager.getPlugin().getServer().getPluginManager().registerEvents(this, combatManager.getPlugin());
        this.populateEntityMappings();
    }

    public void populateEntityMappings(){
        mob_stat_mappings = new EnumMap<>(EntityType.class);
        mob_stat_mappings.put(SKELETON, new MobStats().damage(1.25).speed(1.0, 1.5));
        mob_stat_mappings.put(ZOMBIE, new MobStats().damage(2.0).health(1.5).speed(1.25,2.0));
        mob_stat_mappings.put(DROWNED, new MobStats().damage(1.0).speed(1.5, 1.5).size(0.75, 1.25));
        mob_stat_mappings.put(HUSK, new MobStats().damage(1.0).speed(1.5, 1.5).size(0.75, 1.25));

        mob_stat_mappings.put(CREEPER, new MobStats().damage(1.0).speed(2.0, 1.5));
        mob_stat_mappings.put(SPIDER, new MobStats().damage(1.5).speed(2.0, 1.5).size(0.5, 1.0));
        mob_stat_mappings.put(CAVE_SPIDER, new MobStats().damage(1.0).speed(1.5, 1.5).size(0.75, 1.25));
    }

    EnumMap<EntityType, MobStats> mob_stat_mappings = new EnumMap<>(EntityType.class);
    public MobStats mob_stat_default = new MobStats(2.0, 2.0);



    public MobStats getMobStats(EntityType type) {
        if(mob_stat_mappings.containsKey(type)){
            return mob_stat_mappings.get(type);
        }

        return mob_stat_default;
    }



    @EventHandler
    public void onMobSpawn(EntitySpawnEvent event) {
        if (event.getEntity() instanceof Monster monster) {
            Debug.broadcast("mob", "<dark_red>monster spawned:</dark_red> "+monster.name());
            AttributeInstance attribute = monster.getAttribute(Attribute.MAX_HEALTH);
            MobStats stats = getMobStats(monster.getType());
            double max_health = attribute.getBaseValue() * stats.getHealthMultiplier();
            attribute.setBaseValue(max_health);
            monster.setHealth(max_health);
//            monster.registerAttribute(Attribute.MAX_HEALTH);
//            AttributeInstance attribute = monster.getAttribute(Attribute.MAX_HEALTH);
//            AttributeModifier modifier = attribute.getModifier(new NamespacedKey(Specialization.getInstance(), "max_health"));
//            attribute.addModifier(modifier);

//                double new_value = attr.getBaseValue() * 2.0;
//                attr.setBaseValue(new_value);
//                attr.addModifier(AttributeModifier);
//        if(event.getEntity() instanceof LivingEntity){
//            return; //temporary logic disable
//        }

//            if(monster.getWorld().isDayTime()) speedAddition = SpecializationConfig.getMobConfig().get("DAYTIME_SPEED_BUFF", Double.class);
//            else speedAddition = SpecializationConfig.getMobConfig().get("NIGHTTIME_SPEED_BUFF", Double.class);
//            switch(monster.getType()){
//                case SKELETON -> {
//
//                    monster.getEquipment().getItemInMainHand().setType(Material.STONE_SWORD);
//                }
//            }
            AttributeInstance scale_attribute = monster.getAttribute(Attribute.SCALE);
            if(scale_attribute != null) {
                scale_attribute.setBaseValue(scale_attribute.getBaseValue()*stats.getRandomScale());
            }

            AttributeInstance speed_attribute = monster.getAttribute(Attribute.MOVEMENT_SPEED);
            if(speed_attribute != null) {
                speed_attribute.setBaseValue(speed_attribute.getValue() * (monster.getWorld().isDayTime()?stats.getSpeedMultiplierDay(): stats.getSpeedMultiplierNight()));
            }
            AttributeInstance water_attribute = monster.getAttribute(Attribute.WATER_MOVEMENT_EFFICIENCY);
            if(water_attribute != null) {
                water_attribute.setBaseValue(speed_attribute.getValue() * 2);
            }

            Bukkit.getMobGoals().addGoal(monster,3, new BreakBlockMobGoal(monster));
            Bukkit.getMobGoals().addGoal(monster,0, new TargetPlayerMobGoal(monster));
        }
    }

    /**
     * Amplifies mob damage
     */
    public void onMobAttack(Player player, EntityDamageByEntityEvent event){
        if(!(event.getDamager() instanceof Enemy enemy)) return;

        MobStats stats = getMobStats(enemy.getType());
        double newDamage = event.getDamage(BASE);
        event.setDamage(BASE, newDamage*stats.getDamageMultiplier());

//      BACKUP PLAN FOR MOB DAMAGE:
//        Ensure this method is called after CombatManager's ArmorReduction.
//        double newDamage = event.getDamage();
//        event.setDamage(newDamage*2.0);

//        event.setDamage(EntityDamageEvent.DamageModifier.BASE, newDamage*2);


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
