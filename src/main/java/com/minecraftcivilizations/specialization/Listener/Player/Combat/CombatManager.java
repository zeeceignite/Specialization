package com.minecraftcivilizations.specialization.Listener.Player.Combat;

import com.google.gson.reflect.TypeToken;
import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Listener.Mobs.MobDamage;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.Specialization;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.EnumMap;
import java.util.List;

import static org.bukkit.entity.EntityType.*;

/**
 * The parent manager for everything related to combat, including mob damage
 * Routes damage listeners
 */
public class CombatManager implements Listener {


    private final GuardsmanDamage guardsmanDamage;
//    private final DynamicArmor dynamicArmor; DLC feature by Alectriciti
    private final ArmorDamageReduction armorDamageReduction; // Handles MOB -> PLAYER damage
    private final MobDamage mobDamage;
    private final ArmorEquipAttributes armorEquip;
    private final Berserk berserk; // Berserk Manager

    final Specialization plugin;

    public CombatManager(Specialization specialization) {
        this.plugin = specialization;
        specialization.getServer().getPluginManager().registerEvents(this, specialization);
        guardsmanDamage = new GuardsmanDamage(this);
        mobDamage = new MobDamage(this);
//        dynamicArmor = new DynamicArmor(this);
        armorEquip = new ArmorEquipAttributes(this);
        armorDamageReduction = new ArmorDamageReduction(this);
        berserk = new Berserk(this);
        initializeMobXpMappings();
    }

    @EventHandler
    public void GlobalDamageListener(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            //Attacker is a player
            CustomPlayer customPlayer = CoreUtil.getPlayer(player);
            guardsmanDamage.applyGuardsmanDamage(customPlayer, event);
//            dynamicArmor.applyRaytracedArmorHit(event);
            if(event.getEntity() instanceof LivingEntity victim) {
                applyExp(event, customPlayer, victim); //Exp is acquired only after calculating final damage
            }
        } else {
            //Attacker is a Mob
            // This should ONLY apply to mob damage, not PVP damage
            if(event.getEntity() instanceof Player player) {
//                    Debug.broadcast("damage", "original damage: "+event.getDamager());
                mobDamage.onMobAttack(player, event);
                armorDamageReduction.applyArmorReduction(player, event);
            }
        }
    }



    EnumMap<EntityType, Double> mob_xp_mappings = new EnumMap<>(EntityType.class);

    /**
     * Multipliers for Exp gained from Mob HP
     * might port over to config values later...
     * who knows
     */
    private void initializeMobXpMappings() {
        putXpFor(2, RAVAGER, WITHER, ENDER_DRAGON);
        putXpFor(1.5, PILLAGER, ILLUSIONER, VINDICATOR, EVOKER, ELDER_GUARDIAN, WITCH);
        putXpFor(1.25, CREEPER);
        putXpFor(1.0, ENDERMAN,
                ZOMBIE, HUSK, DROWNED, ZOMBIE_VILLAGER,
                SKELETON, STRAY, BOGGED, WITHER_SKELETON,
                SPIDER, CAVE_SPIDER,
                PHANTOM, BLAZE, BREEZE, GHAST, SHULKER);
        putXpFor(0.5, SLIME, MAGMA_CUBE, SILVERFISH, ENDERMITE, CREAKING, GUARDIAN);
        putXpFor(0.25, PIGLIN_BRUTE, HOGLIN);
    }

    //just a quick init helper
    private void putXpFor(double xp, EntityType...entities){
        for(EntityType e : entities){
            mob_xp_mappings.put(e, xp);
        }
    }

    private void applyExp(EntityDamageByEntityEvent event, CustomPlayer customPlayer, LivingEntity victim) {
        if(event.getDamage()<1)return;
        if (mob_xp_mappings.containsKey(victim.getType())) {
            LivingEntity le = (LivingEntity) victim;
            double xp = event.getDamage();
            if (xp > le.getHealth()) {
                xp = le.getHealth();
            }
            customPlayer.addSkillXp(SkillType.GUARDSMAN, (int) (xp * mob_xp_mappings.get(victim.getType())));
        }
    }

    /**
     * Temporary max health for mobs
     */
    @EventHandler
    public void onMobSpawn(CreatureSpawnEvent event){
//        AttributeInstance attribute = event.getEntity().getAttribute(Attribute.MAX_HEALTH);
//        double max_health = attribute.getValue()*2;
//        attribute.setBaseValue(max_health);
//        event.getEntity().setHealth(max_health);
    }

    /**
     * Custom Mob Drops
     */
    @EventHandler
    public void addCustomMobDrops(EntityDeathEvent e){
        if (e.getEntity().getKiller() != null) {
            Player player = e.getEntity().getKiller();
            assert player != null;
//            CustomPlayer killer = CoreUtil.getPlayer(e.getEntity().getKiller().getUniqueId());
//            EntityType entity = e.getEntity().getType();
            List<NamespacedKey> items = SpecializationConfig.getMobDropsConfig().get(e.getEntityType(), new TypeToken<>() {});
            Material.matchMaterial(e.getEntityType().getKey().getKey());
        }
    }



    /**
     * Calculates the total resulting damage after all Paper/Bukkit modifiers are applied.
     */
    public static double calculateTotalDamage(EntityDamageByEntityEvent event) {
        double total = 0.0;
        for (EntityDamageEvent.DamageModifier modifier : EntityDamageEvent.DamageModifier.values()) {
            try {
                total += event.getDamage(modifier);
            } catch (IllegalArgumentException ignored) {
                // Modifier not applicable for this event
            }
        }
        return total;
    }







}