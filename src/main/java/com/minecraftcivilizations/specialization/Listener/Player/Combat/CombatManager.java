package com.minecraftcivilizations.specialization.Listener.Player.Combat;

import com.google.gson.reflect.TypeToken;
import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Listener.Mobs.MobDamage;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.Specialization;
import com.minecraftcivilizations.specialization.StaffTools.Debug;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredListener;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static org.bukkit.ChatColor.*;
import static org.bukkit.ChatColor.GOLD;
import static org.bukkit.entity.EntityType.*;

import static org.bukkit.event.entity.EntityDamageEvent.DamageModifier.*;
/**
 * The parent manager for everything related to combat, including mob damage
 * Routes damage listeners
 */
public class CombatManager implements Listener {


    public static NamespacedKey CRIT_BONUS_KEY;

    private final GuardsmanDamage guardsmanDamage;
//    private final DynamicArmor dynamicArmor; DLC feature by Alectriciti
    private final ArmorDamageReduction armorDamageReduction; // Handles MOB -> PLAYER damage
    private final MobDamage mobDamage;
    private final ArmorEquipAttributes armorEquip;
    private final Berserk berserk; // Berserk Manager

    @Getter
    final Specialization plugin;

    public CombatManager(Specialization specialization) {
        this.plugin = specialization;
        specialization.getServer().getPluginManager().registerEvents(this, specialization);
        CRIT_BONUS_KEY = new NamespacedKey(specialization, "CRIT_BONUS");
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
        double original_base = event.getDamage(BASE);
        boolean fully_charged = false;
        double charge_amount = -1.0;

        Debug.broadcast("damage", "   "+event.getDamager().getName()+GOLD+" VS "+WHITE+event.getEntity().getName()+"   ");

        if(event.isCritical()) {
            double crit_suppression = event.getDamage()*0.6666; //inverse of 1.5x, extra 6 for safe measure <_<
            event.setDamage(crit_suppression);
        }
        if (event.getDamager() instanceof Player player) {
            //Attacker is a player
            charge_amount = player.getAttackCooldown();
            fully_charged = charge_amount >= 1.0f;
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
                //increase damage of mobs to players
                mobDamage.onMobAttack(player, event);

            }
        }



        //ABSORPTION BEHAVIOR
        double absorption = event.getDamage(ABSORPTION);
        double absorption_to_remove = 0;
        if (absorption < 0) {
            if(event.getDamage(INVULNERABILITY_REDUCTION)==0) {
                if (event.getEntity() instanceof Damageable target) {
                    double absorption_hearts = target.getAbsorptionAmount();
                    if(charge_amount>0.5) {
                        target.setAbsorptionAmount(Math.max(0, absorption_hearts - 1));
                    }
                    event.setDamage(BASE, 0);
                }
            }

//            absorption_to_remove = absorption / 2;
        }


        double blocking_damage = event.getDamage(BLOCKING);
        if(blocking_damage!=0){

        }



        String extramsg = "";
//        CoreUtil.getPlayer(player.customPlayer.getUuid());
//        custom
        if(event.isCritical()){
            double crit_add = 1.5;
            CustomPlayer customPlayer = CoreUtil.getPlayer(event.getDamager().getUniqueId());
            if(customPlayer!=null) {
                int lvl = customPlayer.getSkillLevel(SkillType.GUARDSMAN);
                double base = event.getDamage(BASE);
//                crit_add = Math.min(1.5, 0.2 + Math.pow(1.055, lvl)); //slight exponent boost to crit
                crit_add = 0.5 + (0.125 * (double)lvl);
                double new_base = base + crit_add;
                extramsg += GREEN+" [✨+"+Debug.formatDecimal(crit_add)+"]";
//            new_damage *= (crit_multiplier); //apply custom crit
//            crit_msg = GOLD+" ("+GRAY+"✨ "+GOLD+(Debug.formatDecimal(crit_multiplier) +"x)");

                event.setDamage(BASE, new_base);
            }
        }

        //Finally, apply GLOBAL armor reduction
        if(event.getEntity() instanceof LivingEntity le) {
            armorDamageReduction.applyArmorReduction(le, event);
            if(charge_amount>0.9) {
                extramsg += breakArmorUsingTool(le, event);
            }
        }
//        Debug.broadcast("armor", "");


        double DAMAGE_MINIMUM = 0.15 * original_base;
        if(calculateTotalDamage(event) <= DAMAGE_MINIMUM){
//            event.setCancelled(true);
            Entity entity = event.getEntity();

            for (EntityDamageEvent.DamageModifier m : EntityDamageEvent.DamageModifier.values()) {
                if(event.isApplicable(m))
                event.setDamage(m, 0);
            }
            event.setDamage(BASE, DAMAGE_MINIMUM);
            extramsg += DARK_GRAY+" [Minimum]";
            entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_HEAVY_CORE_PLACE, SoundCategory.PLAYERS, 0.75f,ThreadLocalRandom.current().nextFloat(0.1f)+0.75f);
        }


        //display player CHARGE
        if(charge_amount!=-1.0) {
            extramsg += GOLD + " [⚡" + Debug.formatDecimal(charge_amount) + "]";
        }

        String modifiers = "";

        for (EntityDamageEvent.DamageModifier m : EntityDamageEvent.DamageModifier.values()) {
//            if(event.getDamage(m)!=0)
            modifiers += "\n<gray>"+m.name()+"</gray>: "+Debug.formatDecimal(event.getDamage(m));
        }
        Debug.broadcast(
                "damage",
                DARK_RED+ "Final Damage: "+RED+Debug.formatDecimal(calculateTotalDamage(event))+GRAY+extramsg,
                modifiers
        );

    }


    /**
     * IF the attacker's weapon is an [ArmorBreaker], apply that effect here
     * return using the debug msg
     */
    private String breakArmorUsingTool(LivingEntity victim, EntityDamageByEntityEvent event) {
        // Armor Bonus for EXPERT and above
        if(!(event.getDamager() instanceof LivingEntity attacker)) return "";

        ItemStack itemInMainHand = attacker.getEquipment().getItemInMainHand();
        if(itemInMainHand==null)return "";

        int armor_rolls = 1+ThreadLocalRandom.current().nextInt(3);
        double armor_damage = getArmorBreakAmount(itemInMainHand.getType());
        if(armor_rolls == 0) return "";

        double total_extra_penetration = 0;
            if (event.getDamage(ARMOR) < -0.1) {
                EntityEquipment equipment = victim.getEquipment();
                if(equipment == null) return "[bad equipment]";

                World w = victim.getWorld();

                Set<EquipmentSlot> set = new HashSet<EquipmentSlot>();
                EnumMap<EquipmentSlot, Material> map = new EnumMap<>(EquipmentSlot.class);
                for (int i = 0; i < armor_rolls; i++) {
                    set.add(pickRandomArmorSlot());
                }
                for (EquipmentSlot slot : set) {
                    ItemStack item = equipment.getItem(slot);
                    Material m = ArmorStats.getMaterialBlockType(item.getType());
                    if (m != Material.AIR) {
                        map.put(slot, m);
                        if (item.getType().getMaxDurability() > 0) {
                            // Paper automatically breaks items at 0 durability and plays effects
                            total_extra_penetration += armor_damage;
//                                        total_extra_penetration
                            item.damage((int)armor_damage, attacker);
                            armor_damage*=0.5;
                        }
                    }
                }
                if (total_extra_penetration > 0) {
//                    new_armor = Math.min(0, new_armor + total_extra_penetration);
//                    double mine_hit = 1;

                    //Negate Armor Break from Base Damage
                    event.setDamage(ARMOR, event.getDamage(ARMOR) - 1 - (total_extra_penetration/4));



                    Sound sound = null;
                    for (Map.Entry<EquipmentSlot, Material> slot : map.entrySet()) {
                        Material mat = slot.getValue();
                        if(sound==null){
                            if(mat==Material.SOUL_SOIL){
                                sound = Sound.BLOCK_NYLIUM_FALL;
                            }else if(mat==Material.NETHERITE_BLOCK){
                                sound = Sound.BLOCK_NETHER_BRICKS_BREAK;
                            }else if(mat==Material.CHAIN){
                                sound = Sound.BLOCK_CHAIN_BREAK;
                            }else{
                                if(ThreadLocalRandom.current().nextBoolean()) {
                                    sound = Sound.BLOCK_COPPER_GRATE_HIT;
                                }else{
                                    sound = Sound.BLOCK_COPPER_GRATE_HIT;
                                }
                            }
                        }
                        double y = ArmorStats.getArmorHeight(slot.getKey());
                        w.spawnParticle(Particle.BLOCK, victim.getLocation().add(0, y, 0), (int)total_extra_penetration, 0.125, 0.125, 0.125, 0, mat.createBlockData(), true);
                    }
                    if(sound!=null) {
                        w.playSound(victim.getLocation(), sound, SoundCategory.PLAYERS, 0.95f, 1.2f + ThreadLocalRandom.current().nextFloat(0.2f));
                    }
//                    Particle.DustOptions dust =new Particle.DustOptions(Color color, 10);
//                    w.spawnParticle(Particle.ANGRY_VILLAGER, monster.getEyeLocation(), 4, 0.5,0.6,0.5,0);

            }
        }
            if(total_extra_penetration > 0) {
                return LIGHT_PURPLE + " [⛏" + LIGHT_PURPLE + Debug.formatDecimal(total_extra_penetration) + "] ";
            }else{
                return "";// "bad";
            }
    }


    private EquipmentSlot pickRandomArmorSlot() {
        switch(ThreadLocalRandom.current().nextInt(4)){
            case 0: return EquipmentSlot.HEAD;
            case 1: return EquipmentSlot.CHEST;
            case 2: return EquipmentSlot.LEGS;
            case 3: return EquipmentSlot.FEET;
            default: return null;
        }
    }



    private int getArmorBreakAmount(Material type) {
//        if(type.equals(Material.NETHERITE_PICKAXE)){
//            return 10;
//        }
        if(type.equals(Material.NETHERITE_PICKAXE)){
            return 10;
        }else if(type.equals(Material.DIAMOND_PICKAXE)){
            return 8;
        }else if(type.equals(Material.IRON_PICKAXE)){
            return 6;
        }else if(type.equals(Material.GOLDEN_PICKAXE)){
            return 4;
        }else if(type.equals(Material.STONE_PICKAXE)){
            return 2;
        }else if(type.equals(Material.WOODEN_PICKAXE)){
            return 1;
        }
//        return 0;
        switch(type) {
            case NETHERITE_PICKAXE: return 7;
            case STONE_HOE:
            case GOLDEN_HOE:
            case IRON_HOE:
            case DIAMOND_HOE:
            case NETHERITE_HOE: return 1;
            case STONE_PICKAXE: return 2;
            case IRON_PICKAXE: return 3;
            case GOLDEN_PICKAXE: return 2;
            case DIAMOND_PICKAXE: return 7;
        }
        return 0;
    }


    @EventHandler(priority = EventPriority.MONITOR)
            public void monitorEvents(EntityDamageByEntityEvent event){
        HandlerList handlers = event.getHandlers();
        int i = 0;
        for(RegisteredListener l : handlers.getRegisteredListeners()){
//            Specialization.getInstance().getLogger().info(i+":"+l.getPlugin().getName());
            i++;

        }

    }
    private double safeGet(EntityDamageByEntityEvent event, EntityDamageEvent.DamageModifier mod) {
        try {
            return event.getDamage(mod);
        } catch (IllegalArgumentException ex) {
            return 0.0;
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
            customPlayer.addSkillXp(SkillType.GUARDSMAN, (int) (xp * mob_xp_mappings.get(victim.getType())), true);
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