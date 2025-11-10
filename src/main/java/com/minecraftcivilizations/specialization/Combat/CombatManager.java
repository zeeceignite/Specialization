package com.minecraftcivilizations.specialization.Combat;

import com.google.gson.reflect.TypeToken;
import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Combat.Mobs.MobStatsManager;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.Specialization;
import com.minecraftcivilizations.specialization.StaffTools.Debug;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import com.minecraftcivilizations.specialization.util.PlayerUtil;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.projectiles.ProjectileSource;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static com.minecraftcivilizations.specialization.util.MathUtils.random;
import static org.bukkit.ChatColor.*;
import static org.bukkit.ChatColor.GOLD;
import static org.bukkit.entity.EntityType.*;

import static org.bukkit.event.entity.EntityDamageEvent.DamageModifier.*;
/**
 * The parent manager for everything related to combat, including mob damage
 * Routes damage listeners
 */
public class CombatManager implements Listener {


    public static NamespacedKey ARROW_DAMAGE_KEY;
    public static NamespacedKey CRIT_BONUS_KEY;

    private final GuardsmanDamage guardsmanDamage;
//    private final DynamicArmor dynamicArmor; DLC feature by Alectriciti
    private final ArmorDamageReduction armorDamageReduction; // Handles MOB -> PLAYER damage
    private final MobStatsManager mobDamage;
    private final ArmorEquipAttributes armorEquip;
    private final Berserk berserk; // Berserk Manager
    private final ExplosionDamage explosionDamage;

    @Getter
    final Specialization plugin;

    public CombatManager(Specialization specialization) {
        this.plugin = specialization;
        specialization.getServer().getPluginManager().registerEvents(this, specialization);
        CRIT_BONUS_KEY = new NamespacedKey(specialization, "COMBAT_CRIT_BONUS");
        ARROW_DAMAGE_KEY = new NamespacedKey(specialization, "ARROW_DAMAGE");

        guardsmanDamage = new GuardsmanDamage(this);
        mobDamage = new MobStatsManager(this);
//        dynamicArmor = new DynamicArmor(this);
        armorEquip = new ArmorEquipAttributes(this);
        armorDamageReduction = new ArmorDamageReduction(this);
        berserk = new Berserk(this);
        explosionDamage = new ExplosionDamage(this);
        initializeMobXpMappings();
    }

    @EventHandler
    public void ShootBowListener(ProjectileLaunchEvent event){
        Projectile projectile = event.getEntity();
        if(projectile.getType()==ARROW || projectile.getType()==SPECTRAL_ARROW){
        }else{
            return;
        }

        double multiplier = 1.0;

        ProjectileSource source = projectile.getShooter();
        //TODO firework check
        if(source instanceof LivingEntity shooter){
            ItemStack weapon = shooter.getEquipment().getItemInMainHand();
            switch(weapon.getType()){
                case BOW:
                    if(shooter instanceof Player ps){
                        multiplier = 0.5;
                        ps.setCooldown(Material.CROSSBOW, 16);
                    }else{
                        //skeleton or mob
                        multiplier = 1.25;
                    }
                    break;
                case CROSSBOW:
                    multiplier = 1.5;
                    if(shooter instanceof Player ps){
                        ps.setCooldown(Material.CROSSBOW, 24);
                    }
                    break;
            }
        }else if (source == null) {
            //dispenser
            // Shooter is a dispenser
            multiplier = 2.0;
        }
        projectile.getPersistentDataContainer().set(ARROW_DAMAGE_KEY, PersistentDataType.DOUBLE, multiplier);
    }

    @EventHandler
    public void GlobalDamageListener(EntityDamageByEntityEvent event) {
        double original_base = event.getDamage(BASE);
        boolean fully_charged = false;
        double charge_amount = -1.0;


        /*
            CRIT SUPPRESSION (this allows us to override with our own crit system)
         */
        if(event.isCritical()) {
            double crit_suppression = event.getDamage()*0.6666; //inverse of 1.5x, extra 6 for safe measure <_<
            event.setDamage(crit_suppression);
            original_base = crit_suppression;
        }


        Debug.broadcast("damage", "   "+event.getDamager().getName()+GOLD+" VS "+WHITE+event.getEntity().getName()+"   ");

        Entity damager = event.getDamager();
        if(damager instanceof Projectile projectile){
            if(projectile.getPersistentDataContainer().has(ARROW_DAMAGE_KEY)) {
                double multiplier = projectile.getPersistentDataContainer().get(ARROW_DAMAGE_KEY, PersistentDataType.DOUBLE);
                event.setDamage(BASE, original_base * multiplier);

                String modifiers = "";

                if(event.isApplicable(ARMOR)) {
                    double armor_resist = event.getDamage(ARMOR);
                    event.setDamage(ARMOR, armor_resist*multiplier);
                }

                for (EntityDamageEvent.DamageModifier m : EntityDamageEvent.DamageModifier.values()) {
//            if(event.getDamage(m)!=0)
                    modifiers += "\n<gray>"+m.name()+"</gray>: "+Debug.formatDecimal(event.getDamage(m));
                }
                Debug.broadcast("damage", "Arrow Damage: <red>" + original_base + (event.isCritical()?"<yellow>[CRIT]</yellow>":"") +
                        " <gold>[<gray>🏹</gray>x"+multiplier+"]</gold>"+"</red> Final: <red>" + Debug.formatDecimal(event.getFinalDamage()), modifiers);
            }
        }
        if (damager instanceof Player player) {
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


        CustomPlayer customPlayer = CoreUtil.getPlayer(damager.getUniqueId());

        String extramsg = "";
//        CoreUtil.getPlayer(player.customPlayer.getUuid());
//        custom
        double bonus_crit = 0.0;
        if(event.isCritical()){
            if(customPlayer!=null) {
                if(damager instanceof Player dmger) {
                    ItemStack item = dmger.getEquipment().getItemInMainHand();
                    PlayerUtil u = PlayerUtil.getPlayerUtil(dmger);
                    if (!u.isOnCooldown("crit_bonus") && dmger.getCooldown(item)==0) {
                        bonus_crit = getCustomWeaponCrit(item);
                    }


                    int lvl = customPlayer.getSkillLevel(SkillType.GUARDSMAN);
                    double base = event.getDamage(BASE);
    //                crit_add = Math.min(1.5, 0.2 + Math.pow(1.055, lvl)); //slight exponent boost to crit
                    double crit_add = 0.25 + bonus_crit + (0.125 * (double)lvl);
                    double new_base = base + crit_add;
                    extramsg += "<green> [✨+"+Debug.formatDecimal(crit_add)+"]</green>";
    //            new_damage *= (crit_multiplier); //apply custom crit
    //            crit_msg = GOLD+" ("+GRAY+"✨ "+GOLD+(Debug.formatDecimal(crit_multiplier) +"x)");
                    event.setDamage(BASE, new_base);
                    PlayerUtil.getPlayerUtil(dmger).setCooldown("crit_bonus", 60);
                }
            }
        }

        //Finally, apply GLOBAL armor reduction
        if(event.getEntity() instanceof LivingEntity le) {
            if(event.isApplicable(ARMOR)) {
                armorDamageReduction.applyArmorReduction(le, event);
            }
            extramsg += " <light_purple>"+breakArmorWithItem(le, event)+"</light_purple>";
            if(bonus_crit>0){
                le.getWorld().playSound(le.getLocation(), Sound.ITEM_WOLF_ARMOR_DAMAGE, 0.25f, 1);
            }
//            double blocking_damage = event.getDamage(BLOCKING);
//            if(blocking_damage!=0){
//
////                le.setShieldBlockingDelay(60);
////                if(le instanceof Player pls){
////                    pls.setCooldown(Material.SHIELD, 100);
////                }
//                //TODO blocking stuff
//            }
        }
//        Debug.broadcast("armor", "");


        double DAMAGE_MINIMUM = 0.125 * original_base;
        double total_final = calculateTotalDamage(event);
//        Debug.broadcast("damage", "Pre-Minimu calculation: "+total_final);
        if(total_final <= DAMAGE_MINIMUM) {
//            event.setCancelled(true);
            Entity entity = event.getEntity();


            for (EntityDamageEvent.DamageModifier m : EntityDamageEvent.DamageModifier.values()) {
                if (event.isApplicable(m)) {
                    if(m!=BLOCKING)
                    event.setDamage(m, 0);
                }
            }
            event.setDamage(BASE, DAMAGE_MINIMUM);
            extramsg += " <dark_gray>[Minimum]</dark_gray>";

            Sound sound = ArmorStats.getArmorSound(entity);
            if (sound != null) {
//                extramsg += " <dark_gray>[Sound]</dark_gray>";
                entity.getWorld().playSound(entity.getLocation(), sound, SoundCategory.PLAYERS, 0.75f, ThreadLocalRandom.current().nextFloat(0.1f) + 0.75f);
            }
        }

        //display player CHARGE - ENSURE damager is in survival for testing
        extramsg +=  "<gold> [⚡" + Debug.formatDecimal(charge_amount) + "]</gold>";

        String modifiers = "";

        for (EntityDamageEvent.DamageModifier m : EntityDamageEvent.DamageModifier.values()) {
//            if(event.getDamage(m)!=0)
            modifiers += "\n<gray>"+m.name()+"</gray>: "+Debug.formatDecimal(event.getDamage(m));
        }
        Debug.broadcast(
                "damage",
                "<dark_red>Final Damage: <red>"+Debug.formatDecimal(calculateTotalDamage(event))+extramsg,
                "<red>Minimum Hit Required: </red>"+DAMAGE_MINIMUM+""+modifiers
        );

    }


    /**
     * IF the attacker's weapon is an [ArmorBreaker], apply that effect here
     * return using the debug msg
     */
    private String breakArmorWithItem(LivingEntity victim, EntityDamageByEntityEvent event) {
        // Armor Bonus for EXPERT and above
        if(!(event.getDamager() instanceof Player attacker)) return "";

        if(attacker.getAttackCooldown()<0.425)return "not ready";
        ItemStack itemInMainHand = attacker.getEquipment().getItemInMainHand();
        if(itemInMainHand==null)return "";

        int armor_rolls = 1+ThreadLocalRandom.current().nextInt(3);
        ArmorBreakStats break_stats = getItemArmorBreakStats(itemInMainHand.getType());
        double armor_damage = (double) break_stats.armor;
        if(armor_rolls == 0) return "";

        double total_extra_penetration = 0;
        EntityEquipment equipment = victim.getEquipment();
        if(equipment == null) return "[bad equipment]";
        World w = victim.getWorld();
        if(victim instanceof Player pls) {
                if(event.isApplicable(BLOCKING)) {
                    if (event.getDamage(BLOCKING) < -0.1) {
                        boolean shield_break = false;
                        if (equipment.getItemInOffHand().getType() == Material.SHIELD) {
                            equipment.getItemInOffHand().damage(break_stats.shield, attacker);
                            shield_break = true;
                        }
                        if (equipment.getItemInMainHand().getType() == Material.SHIELD) {
                            equipment.getItemInMainHand().damage(break_stats.shield, attacker);
                            shield_break = true;
                        }
                        if (shield_break) {
                            w.playSound(victim.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, SoundCategory.PLAYERS, 0.895f, 1.1f + ThreadLocalRandom.current().nextFloat(0.2f));
                            w.spawnParticle(Particle.BLOCK, victim.getLocation().add(0, 1, 0), (int) break_stats.shield, 0.25, 0.25, 0.25, 0, Material.PISTON_HEAD.createBlockData(), true);
                            return "[🛡" + break_stats.shield + "]";
                        }
                    }
                }
        }
        if (event.getDamage(ARMOR) < -0.1) {


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
                    total_extra_penetration *= attacker.getAttackCooldown();
                    //Negate Armor Break from Base Damage

//                    event.setDamage(ARMOR, event.getDamage(ARMOR) - 1 - (total_extra_penetration/4));



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
//        if(event.isApplicable(BLOCKING)){
//            double blocking_damage = event.getDamage(BLOCKING);
        if(total_extra_penetration > 0) {
            return "[<gray>⛏</gray>" + LIGHT_PURPLE + Debug.formatDecimal(total_extra_penetration) + "]";
        }else{
            return ""; // "bad";
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




    public class ArmorBreakStats{
        int armor;
        int shield;

        public ArmorBreakStats(int base, int shield){
            this.armor = base;
            this.shield = shield;
        }
    }

    private ArmorBreakStats getItemArmorBreakStats(Material type) {
//        if(type.equals(Material.NETHERITE_PICKAXE)){
//            return 10;
//        }
//        return 0;

        switch(type) {
            case STONE_SWORD: return new ArmorBreakStats(1, 1);
            case STONE_AXE: return new ArmorBreakStats(1, 2);
            case GOLDEN_SWORD: return new ArmorBreakStats(1, 1);
            case GOLDEN_AXE: return new ArmorBreakStats(1, 2);
            case IRON_SWORD: return new ArmorBreakStats(3, 2);
            case IRON_AXE: return new ArmorBreakStats(3, 8);
            case DIAMOND_SWORD: case NETHERITE_SWORD: return new ArmorBreakStats(4, 2);
            case DIAMOND_AXE: case NETHERITE_AXE: return new ArmorBreakStats(4, 12);

            case STONE_HOE:
            case GOLDEN_HOE:
            case IRON_HOE:
            case DIAMOND_HOE:
            case NETHERITE_HOE: return new ArmorBreakStats(1, 1);

            case STONE_PICKAXE: return new ArmorBreakStats(2, 2);
            case GOLDEN_PICKAXE: return new ArmorBreakStats(4, 3);
            case IRON_PICKAXE: return new ArmorBreakStats(5, 4);
            case DIAMOND_PICKAXE: return new ArmorBreakStats(9,5);
            case NETHERITE_PICKAXE: return new ArmorBreakStats(10, 6);
        }
        return new ArmorBreakStats(0,0);
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




    public static double getCustomWeaponCrit(ItemStack weapon){
        if(weapon.hasItemMeta()) {
            if (weapon.getItemMeta().getPersistentDataContainer().has(CRIT_BONUS_KEY)) {
                return weapon.getItemMeta().getPersistentDataContainer().get(CRIT_BONUS_KEY, PersistentDataType.DOUBLE);
            }
        }
        return 0;
    }



}