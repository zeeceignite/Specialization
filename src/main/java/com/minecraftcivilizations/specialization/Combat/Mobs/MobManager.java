package com.minecraftcivilizations.specialization.Combat.Mobs;

import com.minecraftcivilizations.specialization.Combat.CombatManager;
import com.minecraftcivilizations.specialization.MobGoals.BreakBlockMobGoal;
import com.minecraftcivilizations.specialization.MobGoals.TargetPlayerMobGoal;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.Specialization;
import com.minecraftcivilizations.specialization.StaffTools.Debug;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import com.minecraftcivilizations.specialization.util.WorldUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static org.bukkit.event.entity.EntityDamageEvent.DamageModifier.*;
import static org.bukkit.entity.EntityType.*;
import static com.minecraftcivilizations.specialization.util.MathUtils.*;

/**
 * Allows for customization of mob spawning rules and stats
 * I have gone too far with this one
 * @author alectriciti ⚡
 * @see MobOverrideRuleSet for entity_type based mappings
 * @see MobOverrideRule for individual rules for converting a mob into a variant
 * @see MobVariation for specific entity profile settings
 */
public class MobManager implements Listener {

    public static final String TAKEDOWN_CHANCE = "takedown chance: ";
    private final NamespacedKey SPAWN_VARIATION_ID_KEY; //this determines if the mob was overrided
    private final NamespacedKey EXP_GAIN_OVERRIDE_KEY;

    private final NamespacedKey SCALE_KEY;
    private final NamespacedKey MOVE_SPEED_KEY;
    private final NamespacedKey WATER_SPEED_KEY;

    public static MobManager getInstance(){
        return CombatManager.getInstance().getMobManager();
    }

    public MobManager(CombatManager combatManager) {
        Specialization plugin = combatManager.getPlugin();
        this.SPAWN_VARIATION_ID_KEY = new NamespacedKey(plugin, "mob_spawn_id");
        this.EXP_GAIN_OVERRIDE_KEY = new NamespacedKey(plugin, "exp_gain_override");
        SCALE_KEY = new NamespacedKey(plugin, "custom_scale");
        MOVE_SPEED_KEY = new NamespacedKey(plugin, "custom_move_speed");
        WATER_SPEED_KEY = new NamespacedKey(plugin, "custom_water_speed");
        combatManager.getPlugin().getServer().getPluginManager().registerEvents(this, combatManager.getPlugin());
    }


    Map<EntityType, MobOverrideRuleSet> rule_mappings = new HashMap<>();
    Map<String, MobVariation> mob_variations = new HashMap<>();
    MobVariation default_mob_variation;


    void registerRule(MobOverrideRule rule) {
        for (EntityType type : rule.getReplaceTypes()) {
            MobOverrideRuleSet rule_set = rule_mappings.computeIfAbsent(type, k -> new MobOverrideRuleSet(type));
            rule_set.add(rule);
        }
    }

    void setDefaultRuleSetChance(int base_chance, EntityType...types){
        for(EntityType type : types) {
            MobOverrideRuleSet rule_set = rule_mappings.computeIfAbsent(type, k -> new MobOverrideRuleSet(type));
            rule_set.setBaseChance(base_chance);
        }
    }

    void registerMobVariation(MobVariation variation) {
        mob_variations.put(variation.getId(), variation);
    }

    public boolean isMobVariation(Entity entity){
        return entity.getPersistentDataContainer().has(SPAWN_VARIATION_ID_KEY);
    }

    //This simply flags the entity to be a variation
    void convertEntityToVariation(Entity entity, MobVariation variation){
        entity.getPersistentDataContainer().set(SPAWN_VARIATION_ID_KEY, PersistentDataType.STRING, variation.getId());
        if(entity instanceof LivingEntity le){
            applyStatsToEntity(le, variation);
        }
    }

    /**
     * @return A valid mob stat, the default as a fallback
     */
    public MobVariation getMobVariation(Entity e){
        if(e.getPersistentDataContainer().has(SPAWN_VARIATION_ID_KEY)){
            String id = e.getPersistentDataContainer().get(SPAWN_VARIATION_ID_KEY, PersistentDataType.STRING);
            if(mob_variations.containsKey(id)){
                return mob_variations.get(id);
            }
        }
//        Debug.broadcast("mobrule", "using default mob for "+e.getName());
        //TODO return default overrides
        return default_mob_variation;
    }

    /**
     *
     * @return might be null if nothing was found
     */
    public MobOverrideRule rollMobOverrideRule(EntityType type){
        if(rule_mappings.containsKey(type)) {
            Debug.broadcast("mobrule", "<gray>mob rolling for <aqua>"+type+"</aqua>");
            return rule_mappings.get(type).rollRule();
        }
        return null;
    }

//    public final MobVariation zombie_stats = new MobVariation().hunts().breaks().damage(2.0).health(1.0).speed(1.5,2.0);
//    public final MobVariation skeleton_stats = new MobVariation().hunts().damage(1.25).speed(1.0, 1.5);


    /**
     * MobOverrideRule establishes the conditions and rules for which MobVariations get added to the game
     * MobVariations creates a new classification for Custom Mob Variants
     */
    public void populateEntityMappings(){
//        MobOverrideRule.setGlobalChance(100); // This sets the BASE weight chance for ALL entity types, which will avoid rolling for a MobOverrideRule
        //THESE EXIST PRIMARILY FOR REFRESHING
        rule_mappings = new HashMap<>();
        mob_variations = new HashMap<>();
        default_mob_variation = new MobVariation("default_mob").damage(1.5).health(2.0).speed(2.5, 3.5);
        //END OF PRIMARY REFRESH
        setDefaultRuleSetChance(0, ZOMBIE, HUSK, DROWNED, SKELETON, CREEPER, SPIDER); //always override these mobs
//        setDefaultRuleSetChance(10, MAGMA_CUBE, PIGLIN, PIGLIN_BRUTE, HOGLIN, GHAST, BLAZE, WITHER_SKELETON);


        new MobOverrideRule(100, ZOMBIE, HUSK, DROWNED)
                .addVariation(new MobVariation("zombie_variation")
                        .health(2)
                        .damage(2.5, 3.5)
                        .speed(1.25, 1.5)
                        .hunts(64)
                        .breaks()
                        );

        new MobOverrideRule(100, CREEPER)
                .addVariation(new MobVariation("creeper")
                                .xpScale(1.25)
                                .damage(2.5, 3.5)
                                .speed(1.5, 1.5)
                                .hunts()
                        , 1000)
                .addVariation(new MobVariation("quick_creeper")
                        .xpScale(1.5)
                        .damage(1.0)
                        .speed(1.5, 1.75)
                        .hunts()
                , 200);


        new MobOverrideRule(100, SPIDER)
                .addVariation(new MobVariation("spider_small")
                                .health(0.3)
                                .damage(1.5)
                                .speed(1.5, 2.5)
                                .waterspeed(4, 4)
                                .size(0.5,0.5)
                                .spawnExtra(8)
                                .hunts()
                                .drops(0)
                        , 100)
                .addVariation(new MobVariation("spider").damage(1.5)
                                .speed(2.5, 3.5)
                                .stepheight(2.0)
                                .waterspeed(1.5, 1.5)
                                .hunts().drops(0.5, 0.5)
                        , 100);
//                .addVariation(new MobVariation("spider_large", CAVE_SPIDER).health(4).damage(2.0).speed(0.5, 0.75).addImmunity(DamageType.ARROW).size(2.5,2.5).hunts(64).drops(1.0, 2.0).xpScale(1.5)
//                        , 100);

//        MobVariation creeper_variation = new MobVariation("creeper").damage(1.5).speed(1.0, 1.5).hunts();
//        new MobOverrideRule(100, CREEPER)
//                .addVariation(zombie_variation, 10000);
//        MobVariation chaos = new MobVariation("chaos", SHEEP, PIG, COW, WOLF,PIGLIN, PIGLIN_BRUTE).damage(1.0).health(1.5).speed(1.25, 1.25);

//                .addVariation(chaos, 20);

        MobVariation killer_bees = new MobVariation("killer_bees", BEE)
                .anger(true)
                .hunts(32)
                .damage(0.125)
                .health(0.125)
                .speed(2.0, 2.0)
                .size(0.33, 0.44)
                .setGainsXpOverride(true).spawnExtra(2);

        MobVariation wolf_pack = new MobVariation("wolf_pack", WOLF)
                .anger(true)
                .hunts(64)
                .damage(2.5, 3.5)
                .health(1.5)
                .speed(1.5, 1.5)
                .setGainsXpOverride(true)
                .xpScale(1.5).replaceOriginalMob();


        // field spawn
        new MobOverrideRule(5, COW, HORSE)
                .addVariation(killer_bees, 20);

        new MobOverrideRule(50, COW, HORSE)
                .addVariation(wolf_pack, 10);


        setDefaultRuleSetChance(25, POLAR_BEAR);
        new MobOverrideRule(100, POLAR_BEAR)
                .addVariation(new MobVariation("mean_polar_bear", POLAR_BEAR).anger(true).speed(1.2,1.2).health(2).hunts(64));

        new MobOverrideRule(25, ENDERMAN)
                .addVariation(new MobVariation("creaker", CREAKING).anger(true).invisible().health(0.1).hunts(64).replaceOriginalMob());

        // bee DONT DO THIS
//        new MobOverrideRule(100, BEE).spawnInPacks()
//                .addVariation(killer_bees);



        // wolf
        new MobOverrideRule(20, WOLF).spawnInPacks()
                .addVariation(wolf_pack);

        new MobOverrideRule(20, TURTLE).addVariation(new MobVariation("creepo", CREEPER).hunts(32).speed(2,2), 100);

//        new MobOverrideRule(50, ENDERMAN).addVariation(new MobVariation("creakerman", CREAKING).invisible().hunts(32).health(1), 100);



//        MobVariation dolphin_rider = new MobVariation("trident_guy", ZOMBIE, DROWNED, CREEPER)
//                .hunts()
//                .anger(true)
//                .health(2)
//                .setGainsXpOverride(false)
//                .setPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 100000000, 2, false, false, false));

        new MobOverrideRule(20, DOLPHIN, SQUID)
                .spawnInPacks()
                .addVariation(new MobVariation("evil_dolphin", DOLPHIN)
                        .anger(true)
                        .hunts(64)
                        .damage(0.5)
                        .health(4.0)
                        .size(1.25,1.5)
                        .speed(0.75, 1.25)
                        );

//        mob_overrides.put(HORSE, new MobOverrideRule(10).add(BEE, 3).spawnInPacks());
//        mob_overrides.put(COW, new MobOverrideRule(10).add(BEE, 2));
//        mob_overrides.put(PIG, new MobOverrideRule(10).add(WOLF, 10));
//        mob_overrides.put(SQUID, new MobOverrideRule(-*10).add(DOLPHIN, 8));
//        mob_overrides.put(GLOW_SQUID, new MobOverrideRule(10).add(DROWNED, 2).add(DOLPHIN, 8));
//        mob_overrides.put(DOLPHIN, new MobOverrideRule(10).add(GUARDIAN, 2));
//        mob_overrides.put(WITCH, new MobOverrideRule(10).add(ILLUSIONER, 1).add(VINDICATOR, 1));
//        mob_overrides.put(TURTLE, new MobOverrideRule(10).add(CREEPER, 1));
//        mob_overrides.put(ENDERMAN, new MobOverrideRule(10).add(CREAKING, 100));
//
//
//        override_stat_mappings.put(BEE, new MobVariation().health(0.125).size(0.3,0.33).hunts().anger(true).allowXpGainForNonEnemy().xp(2));
//        override_stat_mappings.put(WOLF, new MobVariation().anger(true).hunts().size(1.125f,1.225f).waterspeed(1.5f,1.5f).speed(1.25f,1.25f).allowXpGainForNonEnemy().xp(2));
//        override_stat_mappings.put(DOLPHIN, new MobVariation().anger(true).damage(1).speed(1.5f,1.5f).xp(2).allowXpGainForNonEnemy());
//        override_stat_mappings.put(GUARDIAN, new MobVariation().health(1.0f));
//        override_stat_mappings.put(CREAKING, new MobVariation().damage(2).invisible().xp(10));
//
//
//        vanilla_stat_mappings.put(SKELETON, skeleton_stats);
//        vanilla_stat_mappings.put(BOGGED, skeleton_stats);
//        vanilla_stat_mappings.put(STRAY, skeleton_stats);
//        vanilla_stat_mappings.put(ZOMBIE, zombie_stats);
//        vanilla_stat_mappings.put(DROWNED, zombie_stats);
//        vanilla_stat_mappings.put(HUSK, zombie_stats);


//        vanilla_stat_mappings.put(GHAST, new MobStats().hunts().health(2));

//        vanilla_stat_mappings.put(CREEPER, new MobVariation().damage(1.0).speed(2.0, 1.5));
//        vanilla_stat_mappings.put(SPIDER, new MobVariation().damage(1.5).speed(1.5, 1.5).waterspeed(1.5,2).size(0.9, 1.0));
//        vanilla_stat_mappings.put(CAVE_SPIDER, new MobVariation().damage(1.0).speed(1.5, 1.5).size(0.5, 1.0));
    }


//    public MobVariation getOverrideMobStats(EntityType type) {
//        if(override_stat_mappings.containsKey(type)){
//            return override_stat_mappings.get(type);
//        }
//        return mob_stat_default;
//    }
//
//    public MobVariation getVanillaMobStats(EntityType type) {
//        if(vanilla_stat_mappings.containsKey(type)){
//            return vanilla_stat_mappings.get(type);
//        }
//        return mob_stat_default;
//    }

    @EventHandler(priority = EventPriority.HIGH) //(ignoreCancelled = true)
    public void onChunkPopulate(ChunkPopulateEvent event) {
        List<LivingEntity> list = new ArrayList<LivingEntity>();
        for(Entity e : event.getChunk().getEntities()){
            if(e instanceof LivingEntity livingEntity){
                list.add(livingEntity);
            }
        }
        Bukkit.getScheduler().runTask(Specialization.getInstance(), () -> overrideMobs(list));
    }

    /**
     * This allows mob overrides to be spawned in clusters using natural chunk generation
     * note: this is essentially tagging along existing mob spawns
     */
    private void overrideMobs(List<LivingEntity> list) {
        // pre-determine which categories will be overridden (and to what)
        //get a ruleset for EACH entity type

        // gather unique entity types present in this chunk/list
        Set<EntityType> present_types = list.stream()
                .map(LivingEntity::getType)
                .collect(Collectors.toSet());

        // if the ruleset does not spawn in a pack, assign that here
        Map<EntityType, MobOverrideRule> replacement_map = new HashMap<>();
        // if the ruleset spawns in a pack, we'll assign that here
        Map<EntityType, MobVariation> spawn_in_pack = new HashMap<>();

        for (EntityType type : present_types) {
            //roll a new rule, apply it to
            MobOverrideRule rule = rollMobOverrideRule(type);
            if(rule!=null) {
                if (rule.doesSpawnInPacks()) {
                    spawn_in_pack.put(type, rule.rollVariation()); // This will cause ALL the entities of this type to roll as a specific variation
                } else {
                    replacement_map.put(type, rule); // This will cause the entities to roll a new variation per entity
                }
            }
        }

        // apply the  results to every mob in the list
        for (LivingEntity living : list) {
            EntityType type_original = living.getType();
            MobVariation variation = null;
            Location loc = living.getLocation();
            if(spawn_in_pack.containsKey(type_original)){
                variation = spawn_in_pack.get(type_original);
            }else if(replacement_map.containsKey(type_original)){
                variation = replacement_map.get(type_original).rollVariation();
            }
            if(variation!=null){
                EntityType new_type = variation.rollType();
                if(new_type == null){
                    //apply the stats now, as we do not override type
                    convertEntityToVariation(living, variation);
                    Debug.broadcast("mob", MiniMessage.miniMessage().deserialize("<blue>[CHNK_NULL]</blue> <yellow>["+variation.getId()+"]</yellow> applying to <gray>"+living.getName()+"</gray> at ").append(Debug.formatLocationClickable(living.getLocation(),true)));
                }else{
                    if(variation.doesReplaceOriginalMob()) {
                        living.remove(); // this deletes the existing mob
                    }else{
                        loc = WorldUtils.createRandomLocationInChunk(loc);
                        loc = WorldUtils.getNextSafeVerticalPosition(loc).add(0, 1,0);
                    }

                    //Spawn a new mob with the variation settings
                    Entity e = loc.getWorld().spawnEntity(loc, new_type, CreatureSpawnEvent.SpawnReason.CUSTOM);
                    convertEntityToVariation(e, variation);

                    Debug.broadcast("mob", MiniMessage.miniMessage().deserialize("<light_purple>[CHNK_VALID]</light_purple> <yellow>["+variation.getId()+"]</yellow> <gray>"+living.getName()+"</gray> to <gray>"+e.getName()+"</gray> at ").append(Debug.formatLocationClickable(e.getLocation(),true)));

                }
//                Debug.broadcast("mob", "Chunk Gen Override:<light_purple>" + type_original.name() +
//                        "</light_purple> -> <green>" + variation.getId() + "</green>");
            }
        }
    }


    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {

        if(event.isCancelled())return;
        LivingEntity entity = event.getEntity();
        EntityType type = entity.getType();
//        Debug.broadcast("mob","summoning "+event.getSpawnReason().name());


        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM) {
            //THIS IS A NATURAL GAME SPAWN
            MobOverrideRule rule = rollMobOverrideRule(type);
            if (rule != null) {
                MobVariation variation = rule.rollVariation();
                if (variation != null) {
                    EntityType new_type = variation.rollType();
                    Location loc = entity.getLocation();
                    if (new_type == null) {
                        //apply the stats now, as we do not override type
                        convertEntityToVariation(entity, variation);
                        Debug.broadcast("mob", MiniMessage.miniMessage().deserialize("<DARK_GREEN>[CS_NULL]</DARK_GREEN> <yellow>[" + variation.getId() + "]</yellow> to <gray>" + entity.getName() + "</gray> at ").append(Debug.formatLocationClickable(entity.getLocation(), true)));
                    } else {
                        if (variation.doesReplaceOriginalMob()) {
                            event.setCancelled(true);
                        } else {
                            loc = WorldUtils.createRandomLocationInChunk(loc);
                            loc = WorldUtils.getNextSafeVerticalPosition(loc);
                        }
                        //Spawn a new mob with the variation settings
                        Entity e = loc.getWorld().spawnEntity(loc, new_type, CreatureSpawnEvent.SpawnReason.CUSTOM);
                        if (e instanceof LivingEntity le) {
                            convertEntityToVariation(le, variation);
                            Debug.broadcast("mob", MiniMessage.miniMessage().deserialize("<green>[CS_VALID]</green> <yellow>[" + variation.getId() + "]</yellow> <gray>" + entity.getName() + "</gray> to <gray>" + e.getName() + "</gray> at ").append(Debug.formatLocationClickable(entity.getLocation(), true)));
                            MobVariation mount_variation = variation.getMount();
                            if (mount_variation != null) {
                                if (variation.getMountChance() > ThreadLocalRandom.current().nextDouble()) {
                                    Entity ee = loc.getWorld().spawnEntity(loc, mount_variation.rollType(), CreatureSpawnEvent.SpawnReason.CUSTOM);
                                    convertEntityToVariation(ee, mount_variation);
                                    le.addPassenger(ee);
                                }
                            }
                            if(variation.getSpawnExtra()>0){
                                for(int i = 0; i < variation.getSpawnExtra(); i++){
                                    Entity ee = loc.getWorld().spawnEntity(loc, new_type, CreatureSpawnEvent.SpawnReason.CUSTOM);
                                    convertEntityToVariation(ee, variation);
                                }
                            }
                        }
                    }
                }
            }
        }else{

        }

        if (Debug.isAnyoneListening("mob", false) || Debug.isAnyoneListening("mobrule", false)) {
            populateEntityMappings();
//                Debug.broadcast("mob", "repopulating mob stats!");
        }
    }




        /**
         * TESTING ZONE
         */

    /**
     * Explicitly adds the stats to an entity
     */
    public void applyStatsToEntity(LivingEntity entity, MobVariation stats) {
        entity.setPersistent(true);
        boolean is_day_time = entity.getWorld().isDayTime();

        AttributeInstance attribute = entity.getAttribute(Attribute.MAX_HEALTH);
        double max_health = attribute.getBaseValue() * stats.getHealthMultiplier();
        attribute.setBaseValue(max_health);
        entity.setHealth(max_health);

        double applyspeed_modifier = 1.0;
        if(entity instanceof Ageable ageable){
            //TODO set baby override
            if(!ageable.isAdult()){
                applyspeed_modifier = 0.75;
            }
        }
        //DO NOT USE THIS. USE ENTITY DAMAGE EVNET INSTEAD
//        AttributeInstance damage_attribute = entity.getAttribute(Attribute.ATTACK_DAMAGE);
//        if(damage_attribute != null) {
//            damage_attribute.setBaseValue(damage_attribute.getBaseValue() * (is_day_time?stats.getDamageMultiplierDay():stats.getDamageMultiplierNight()));
//        }
        // SCALE
        AttributeInstance scale_attr = entity.getAttribute(Attribute.SCALE);
        if (scale_attr != null) {
            double mult = stats.getScale();          // e.g. 1.3
            double amount = mult - 1.0;              // ADD_SCALAR expects +0.3

            scale_attr.removeModifier(SCALE_KEY);
            scale_attr.addModifier(new AttributeModifier(
                    SCALE_KEY,
                    amount,
                    AttributeModifier.Operation.ADD_SCALAR
            ));
        }

        // MOVEMENT SPEED
        AttributeInstance speed_attr = entity.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed_attr != null) {
            double mult = applyspeed_modifier *
                    (is_day_time ? stats.getSpeedMultiplierDay() : stats.getSpeedMultiplierNight());
            double amount = mult - 1.0;

            speed_attr.removeModifier(MOVE_SPEED_KEY);
            speed_attr.addModifier(new AttributeModifier(
                    MOVE_SPEED_KEY,
                    amount,
                    AttributeModifier.Operation.ADD_SCALAR
            ));
        }

        // WATER SPEED
        AttributeInstance water_attr = entity.getAttribute(Attribute.WATER_MOVEMENT_EFFICIENCY);
        if (water_attr != null) {
            double mult = applyspeed_modifier *
                    (is_day_time ? stats.getWaterSpeedMultiplierDay() : stats.getWaterSpeedMultiplierNight());
            double amount = mult - 1.0;

            water_attr.removeModifier(WATER_SPEED_KEY);
            water_attr.addModifier(new AttributeModifier(
                    WATER_SPEED_KEY,
                    amount,
                    AttributeModifier.Operation.ADD_SCALAR
            ));
        }
        if(stats.isInvisible()){
            entity.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false, false));
        }
        if(stats.potionEffect!=null) {
            entity.addPotionEffect(stats.potionEffect);
        }
        applyLogicToMob(entity, stats);
    }

    /**
     * Called on mob creation AND on mob load to reapply logic
     */
    private static void applyLogicToMob(LivingEntity entity, MobVariation stats) {
        if(stats.isAngry()) {
            if (entity instanceof Bee bee) {
                bee.setAnger(1000000);
            }else if(entity instanceof Wolf wolf) {
                wolf.setAngry(true);
            }else if(entity instanceof PolarBear bear){
                bear.setAggressive(true);
                bear.setStanding(true);
            }
            if(entity instanceof Mob mob){
                //includes dolphins etc
                mob.setAggressive(true);
            }
        }
        if(stats.doesBreaking()) {
            if (entity instanceof Monster monster) {
                Bukkit.getMobGoals().addGoal(monster, 3, new BreakBlockMobGoal(monster));
            }
        }
        if(stats.doesHunting()) {
            if (entity instanceof Mob mob) {
                Bukkit.getMobGoals().addGoal(mob, 0, new TargetPlayerMobGoal(mob, stats.getFollowRange()));
            }
        }
    }


    @EventHandler
    public void onEntityLoad(EntitiesLoadEvent event){
        List<Entity> entities = event.getEntities();
        for(Entity e : entities){
            if(isMobVariation(e)){
                if(e instanceof LivingEntity le) {
//                    Specialization.getInstance().getLogger().info("applying logic to "+e.getName());
                    applyLogicToMob(le, getMobVariation(le));
                }
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event){
        Entity damager = event.getDamager();
        Entity victim = event.getEntity();

        if(isMobVariation(victim)){
            MobVariation variation = getMobVariation(victim);
            DamageType event_type = event.getDamageSource().getDamageType();
            for(DamageType immunity : variation.immunity_types) {
                if (event_type.equals(immunity)){
                    event.setCancelled(true);
                    return;
                }
            }
//            Debug.broadcast("mob", "Mob is Variation: <green>"+variation.getId()+"");
        }


        // MOB TAKE DAMAGE
        switch(victim.getType()){
            case CREAKING:
                if(isMobVariation(victim)){
                    double takedown_chance=0.25;
                    CustomPlayer player = CoreUtil.getPlayer(event.getDamager().getUniqueId());
                    if(player!=null){
                        takedown_chance = 0.25 + (((double)player.getSkillLevel(SkillType.GUARDSMAN))/10);
                    }
//                    Debug.broadcast("mob", TAKEDOWN_CHANCE+takedown_chance);
                    if(ThreadLocalRandom.current().nextDouble() > takedown_chance){
                        event.setCancelled(true);
                        Location teleport;
                        Vector v = getDirectionVector(victim.getYaw(), victim.getPitch()).normalize();
                        v = v.add(randomVectorCentered(1));
                        teleport = victim.getLocation().add(v.multiply(random(10, 20)));
                        Block block = teleport.getBlock();
                        while(block.isSolid()){
                            block = block.getRelative(BlockFace.UP);
                        }
                        teleport = block.getRelative(BlockFace.UP).getLocation().add(0.5,0.5,0.5);
                        victim.teleport(teleport);
                        victim.setFallDistance(-100);
                        victim.getWorld().playSound(teleport, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);

                    }
                }
                break;
        }
        switch(damager.getType()){
            case BEE:
                if(isMobVariation(damager)) {
                    Bee bee = (Bee) event.getDamager();
                    bee.setHasStung(false);
                    bee.getServer().getScheduler().scheduleSyncDelayedTask(Specialization.getInstance(), new Runnable() {
                        @Override
                        public void run() {
                            unsetBee(bee);
                        }
                    });
                }
                break;
        }
    }

    private void unsetBee(Bee bee) {
//        Debug.broadcast("mob", "Be has stung = false");
        bee.setHasStung(false);
    }

    /**
     * Amplifies mob damage
     * called from CombatManger
     */
    public void onMobAttack(Player player, EntityDamageByEntityEvent event){
        if(!(event.getDamager() instanceof LivingEntity entity)) return;

        MobVariation stats = getMobVariation(entity);
        if(stats == default_mob_variation) return;


        /**
         * Scales mob damage based on their day/night settings
         */
        double newDamage = event.getDamage(BASE);
        double mob_damage_multiplier = entity.getWorld().isDayTime()?stats.getDamageMultiplierDay():stats.getDamageMultiplierNight();
        double mob_damage_base_increase = entity.getWorld().isDayTime()?stats.getDamageBaseDay():stats.getDamageBaseNight();
        event.setDamage(BASE, (newDamage * mob_damage_multiplier) + mob_damage_base_increase);

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


    @EventHandler
    public void onDeath(EntityDeathEvent event){
        LivingEntity entity = event.getEntity();
        if(isMobVariation(entity)){
            MobVariation variation = getMobVariation(entity);
            if(variation.dropChance != -1){
                if(variation.dropChance > ThreadLocalRandom.current().nextDouble()) {
                    //get the drop
                }else{
                    event.getDrops().clear();
                    return;
                }
            }
            if(variation.dropAmountScale!=1.0){
                for (ItemStack item : event.getDrops()) {
                    int scaledAmount = Math.max(1, (int) Math.round(item.getAmount() * variation.dropAmountScale));
                    item.setAmount(scaledAmount);
                }
            }
        }
    }



    public void applyExp(EntityDamageByEntityEvent event, CustomPlayer customPlayer, LivingEntity victim) {
        if(event.getDamage()<0.1)return;

        boolean does_grant_exp = true;

        MobVariation mobStats = getMobVariation(victim);
        if(victim.getPersistentDataContainer().has(EXP_GAIN_OVERRIDE_KEY)){
            Debug.broadcast("mobxp", "grant exp from PDC");
            does_grant_exp = victim.getPersistentDataContainer().get(EXP_GAIN_OVERRIDE_KEY, PersistentDataType.BOOLEAN);
        }else {
            if(mobStats.isXpGainOverrideActive()) {
                Debug.broadcast("mobxp", "grant exp OVERRIDE ACTIVE");
                does_grant_exp = mobStats.getXpGainOverrideState();
             }else if(victim instanceof  Enemy){
                Debug.broadcast("mobxp", "grant exp because is enemy");
                does_grant_exp = true;
            }else{
                does_grant_exp = false;
            }
        }

        // if entity does not grant exp, exit
        if(!does_grant_exp){
            return;
        }

        Debug.broadcast("mobxp", "APPLYING EXP");

        double xp_multiplier = mobStats.getXpScale();
        if (xp_multiplier>0) {
            LivingEntity le = (LivingEntity) victim;
            double xp = event.getDamage();
            if (xp > le.getHealth()) {
                xp = le.getHealth();
            }
            customPlayer.addSkillXp(SkillType.GUARDSMAN, (int) (xp * xp_multiplier), true);
        }
    }

    /**
     * This will force a specific entity to grant Guardsman Exp when damaged
     * This takes highest priority, by overriding the MobVariation rulesset
     * @param entity
     */
    public static void setExpGainOverride(Entity entity, boolean gives_exp){
        NamespacedKey preventExpGainKey = getInstance().EXP_GAIN_OVERRIDE_KEY;
        entity.getPersistentDataContainer().set(preventExpGainKey, PersistentDataType.BOOLEAN, gives_exp);
    }



}
