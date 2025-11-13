package com.minecraftcivilizations.specialization.Combat.Mobs;

import com.minecraftcivilizations.specialization.Combat.CombatManager;
import com.minecraftcivilizations.specialization.MobGoals.BreakBlockMobGoal;
import com.minecraftcivilizations.specialization.MobGoals.TargetPlayerMobGoal;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.Specialization;
import com.minecraftcivilizations.specialization.StaffTools.Debug;
import io.papermc.paper.event.packet.PlayerChunkLoadEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.stream.Collectors;

import static org.bukkit.event.entity.EntityDamageEvent.DamageModifier.*;
import static org.bukkit.entity.EntityType.*;

/**
 * Allows for customization of mob spawning rules and stats
 * @author alectriciti ⚡
 */
public class MobStatsManager implements Listener {

    CombatManager combatManager;
    private final NamespacedKey SPAWN_OVERRIDE_KEY; //this determines if the mob was overrided

    public MobStatsManager(CombatManager combatManager) {
        this.combatManager = combatManager;
        this.SPAWN_OVERRIDE_KEY = new NamespacedKey(combatManager.getPlugin(), "mob_spawn_override");
        this.combatManager.getPlugin().getServer().getPluginManager().registerEvents(this, combatManager.getPlugin());
        this.populateEntityMappings();
    }

    EnumMap<EntityType, MobOverride> mob_overrides = new EnumMap<>(EntityType.class);
    EnumMap<EntityType, MobStats> vanilla_stat_mappings = new EnumMap<>(EntityType.class);
    EnumMap<EntityType, MobStats> override_stat_mappings = new EnumMap<>(EntityType.class);

    public MobStats mob_stat_default = new MobStats(1.5, 1.5);

    public final MobStats zombie_stats = new MobStats().hunts().breaks().damage(2.0).health(1.0).speed(1.5,2.0);
    public final MobStats skeleton_stats = new MobStats().hunts().damage(1.25).speed(1.0, 1.5);



    public void populateEntityMappings(){
        vanilla_stat_mappings = new EnumMap<>(EntityType.class);
        override_stat_mappings = new EnumMap<>(EntityType.class);
        mob_overrides = new EnumMap<>(EntityType.class);

        mob_overrides.put(SHEEP, new MobOverride(10).add(BEE, 10));
        mob_overrides.put(COW, new MobOverride(90).add(RAVAGER, 10));
        mob_overrides.put(PIG, new MobOverride(10).add(WOLF, 10));
        mob_overrides.put(SQUID, new MobOverride(10).add(DROWNED, 2).add(PUFFERFISH, 3));
        mob_overrides.put(GLOW_SQUID, new MobOverride(10).add(DROWNED, 2).add(PUFFERFISH, 3));
        mob_overrides.put(DOLPHIN, new MobOverride(10).add(GUARDIAN, 2));
        mob_overrides.put(WITCH, new MobOverride(10).add(ILLUSIONER, 1).add(VINDICATOR, 1));
        mob_overrides.put(TURTLE, new MobOverride(10).add(SLIME, 2).add(CREEPER, 1));
        mob_overrides.put(ENDERMAN, new MobOverride(10).add(CREAKING, 100));

        override_stat_mappings.put(BEE, new MobStats().health(0.125).size(0.3,0.33).hunts().anger(true));
        override_stat_mappings.put(WOLF, new MobStats().anger(true).hunts().size(1.225f,1.225f).waterspeed(1.5f,1.5f).speed(1.25f,1.25f));
        override_stat_mappings.put(GUARDIAN, new MobStats().health(1.0f));
//        override_stat_mappings.put(CREAKING, new MobStats().health(1.0f));


        vanilla_stat_mappings.put(SKELETON, skeleton_stats);
        vanilla_stat_mappings.put(BOGGED, skeleton_stats);
        vanilla_stat_mappings.put(STRAY, skeleton_stats);
        vanilla_stat_mappings.put(ZOMBIE, zombie_stats);
        vanilla_stat_mappings.put(DROWNED, zombie_stats);
        vanilla_stat_mappings.put(HUSK, zombie_stats);


        vanilla_stat_mappings.put(CREEPER, new MobStats().damage(1.0).speed(2.0, 1.5));
        vanilla_stat_mappings.put(SPIDER, new MobStats().damage(1.5).speed(1.5, 1.5).waterspeed(1.5,2).size(0.9, 1.0));
        vanilla_stat_mappings.put(CAVE_SPIDER, new MobStats().damage(1.0).speed(1.5, 1.5).size(0.5, 1.0));
    }


    public MobStats getOverrideMobStats(EntityType type) {
        if(override_stat_mappings.containsKey(type)){
            return override_stat_mappings.get(type);
        }
        return mob_stat_default;
    }

    public MobStats getVanillaMobStats(EntityType type) {
        if(vanilla_stat_mappings.containsKey(type)){
            return vanilla_stat_mappings.get(type);
        }
        return mob_stat_default;
    }

    @EventHandler
    public void onCreatureSpawn(EntitySpawnEvent event) {
//        if(event.getEntity() instanceof LivingEntity)
//        Debug.broadcast("mob", "entityspawnevent: "+event.getEntity().getType().name());
    }


    @EventHandler
    public void onChunkGen(PlayerChunkLoadEvent event){
//        event.
    }

    @EventHandler(ignoreCancelled = true)
    public void onChunkPopulate(ChunkPopulateEvent event) {
        List<LivingEntity> list = new ArrayList<LivingEntity>();
        for(Entity e : event.getChunk().getEntities()){
            if(e instanceof LivingEntity livingEntity){
                list.add(livingEntity);
            }
        }
        Bukkit.getScheduler().runTask(Specialization.getInstance(), () -> overrideMobs(list));
        // chunk might not be fully safe to inspect right now — run next tick on main thread
//        Chunk chunk = event.getChunk();
//        Bukkit.getScheduler().runTask(plugin, () -> scanChunkForMobs(chunk));
    }

    private void overrideMobs(List<LivingEntity> list) {
        // pre-determine which categories will be overridden (and to what)
        Map<EntityType, Boolean> does_override = new HashMap<>();
        Map<EntityType, EntityType> replacement_map = new HashMap<>();

        // gather unique entity types present in this chunk/list
        Set<EntityType> present_types = list.stream()
                .map(LivingEntity::getType)
                .collect(Collectors.toSet());

        for (EntityType original : present_types) {
            MobOverride override = mob_overrides.get(original);
            if (override != null) {
                EntityType rolled = override.rollType(); // roll once per category
                boolean will_override = rolled != null;
                does_override.put(original, will_override);
                if (will_override) replacement_map.put(original, rolled);
            } else {
                does_override.put(original, false);
            }
        }

        // apply the pre-determined results to every mob in the list
        for (LivingEntity living : list) {
            EntityType original = living.getType();
            if (Boolean.TRUE.equals(does_override.get(original))) {
                EntityType to_spawn = replacement_map.get(original);
                if (to_spawn != null) {
                    //OVERRIDE MOB
                    Location loc = living.getLocation();
                    Entity e = loc.getWorld().spawnEntity(loc, to_spawn, CreatureSpawnEvent.SpawnReason.CUSTOM);
                    e.getPersistentDataContainer().set(SPAWN_OVERRIDE_KEY, PersistentDataType.BOOLEAN, true); // this is applied, but not detected in onCreatureSpawn
                    e.setPersistent(true);
                    living.remove();
                    Debug.broadcast("mob", "Chunk Gen Override:<light_purple>" + original.name() +
                            "</light_purple> -> <green>" + to_spawn.name() + "</green>");
                }
            }
        }
    }


    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event){
        if(event.getDamager().getType()==BEE){
            Bee bee = (Bee) event.getDamager();
            bee.setHasStung(false);
            bee.getServer().getScheduler().scheduleSyncDelayedTask(Specialization.getInstance(), new Runnable() {
                @Override
                public void run() {
                    unsetBee(bee);
                }
            });
        }
    }

    private void unsetBee(Bee bee) {
        Debug.broadcast("mob", "Be has stung = false");
        bee.setHasStung(false);
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        LivingEntity entity = event.getEntity();
        EntityType type = entity.getType();
        Debug.broadcast("mob", MiniMessage.miniMessage().deserialize("Spawning <yellow>"+entity.getType().name()+"</yellow> <gold>("+event.getSpawnReason().name()+")</gold> at ").append(Debug.formatLocationClickable(event.getLocation(), true)), MiniMessage.miniMessage().deserialize(""));

        //        Debug.broadcast("mob",+Debug.formatLocation(event.getLocation()));
        if(event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM) {
            if (mob_overrides.containsKey(entity.getType())) {
                MobOverride override = mob_overrides.get(entity.getType());
                EntityType new_type = override.rollType();
                if (new_type != null) {
                    //MOB OVERRIDE BEING MADE
                    event.setCancelled(true);
                    Location loc = event.getLocation();
                    Entity e = loc.getWorld().spawnEntity(loc, new_type, CreatureSpawnEvent.SpawnReason.CUSTOM);
                    e.getPersistentDataContainer().set(SPAWN_OVERRIDE_KEY, PersistentDataType.BOOLEAN, true);
                    Debug.broadcast("mob", "Spawn Override:<yellow>" + event.getEntityType().name() + "</yellow> -> <green>" + new_type.name() + "</green>");
                    return;
                }
            }
        }
        if(Debug.isAnyoneListening("mob", false)){
            populateEntityMappings();
//                Debug.broadcast("mob", "repopulating mob stats!");
        }

        /**
         * TESTING ZONE
         */

        MobStats stats;// = getApplicableMobStats(entity);
        //THESE ARE OVERRIDES
        if(event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM) {
            stats = getOverrideMobStats(type);
        }else{
            stats = getVanillaMobStats(type);
        }

        applyStatsToEntity(entity, stats);
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
    }

    private static void applyStatsToEntity(LivingEntity entity, MobStats stats) {

        AttributeInstance attribute = entity.getAttribute(Attribute.MAX_HEALTH);
        double max_health = attribute.getBaseValue() * stats.getHealthMultiplier();
        attribute.setBaseValue(max_health);
        entity.setHealth(max_health);

        double applyspeed_modifier = 1.0;
        if(entity instanceof Ageable ageable){
            if(!ageable.isAdult()){
                applyspeed_modifier = 0.75;
            }
        }
        AttributeInstance scale_attribute = entity.getAttribute(Attribute.SCALE);
        if(scale_attribute != null) {
            scale_attribute.setBaseValue(scale_attribute.getBaseValue() * stats.getRandomScale());
        }

        AttributeInstance speed_attribute = entity.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed_attribute != null) {
            speed_attribute.setBaseValue(speed_attribute.getBaseValue() * applyspeed_modifier*(entity.getWorld().isDayTime() ? stats.getSpeedMultiplierDay() : stats.getSpeedMultiplierNight()));
        }
        AttributeInstance water_attribute = entity.getAttribute(Attribute.WATER_MOVEMENT_EFFICIENCY);
        if (water_attribute != null) {
            water_attribute.setBaseValue(water_attribute.getBaseValue() * applyspeed_modifier*(entity.getWorld().isDayTime() ? stats.getWaterSpeedMultiplierDay() : stats.getWaterSpeedMultiplierNight()));
        }



        if(stats.isAngry()) {
            if (entity instanceof Bee bee) {
                bee.setAnger(1000000);
            }else if(entity instanceof Wolf wolf){
                wolf.setAngry(true);
            }
            if(entity instanceof Mob mob){
                //includes dolphins etc
                Debug.broadcast("mob", mob.getType().name()+" is <dark_red>AGGRESSIVE</dark_red>");
                mob.setAggressive(true);
                Bukkit.getMobGoals().addGoal(mob,0, new TargetPlayerMobGoal(mob));
            }

        }

        if(stats.doesBreaking()) {
            if (entity instanceof Monster monster) {
                Bukkit.getMobGoals().addGoal(monster, 3, new BreakBlockMobGoal(monster));
            }
        }
        if(stats.doesHunting()) {
            if (entity instanceof Mob mob) {
                Bukkit.getMobGoals().addGoal(mob, 0, new TargetPlayerMobGoal(mob));
            }
        }

    }

    /**
     * Amplifies mob damage
     */
    public void onMobAttack(Player player, EntityDamageByEntityEvent event){
        if(!(event.getDamager() instanceof LivingEntity entity)) return;

        MobStats stats = getApplicableMobStats(entity);
        if(stats==mob_stat_default)return;

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

    public void applyExp(EntityDamageByEntityEvent event, CustomPlayer customPlayer, LivingEntity victim) {
        if(event.getDamage()<1)return;

        MobStats mobStats = getApplicableMobStats(victim);
        double xp_multiplier = mobStats.getXpMultiplier();
        if (xp_multiplier>0) {
            LivingEntity le = (LivingEntity) victim;
            double xp = event.getDamage();
            if (xp > le.getHealth()) {
                xp = le.getHealth();
            }
            customPlayer.addSkillXp(SkillType.GUARDSMAN, (int) (xp * xp_multiplier), true);
        }
    }

    public MobStats getApplicableMobStats(Entity e){
        if(isOverrideEntity(e)){
            return getOverrideMobStats(e.getType());
        }else{
            return getVanillaMobStats(e.getType());
        }
    }

    public boolean isOverrideEntity(Entity e){
        if(e.getPersistentDataContainer().has(SPAWN_OVERRIDE_KEY)){
            return e.getPersistentDataContainer().get(SPAWN_OVERRIDE_KEY, PersistentDataType.BOOLEAN);
        }
        return false;
    }

}
