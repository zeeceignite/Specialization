package com.minecraftcivilizations.specialization.Listener.Player.Combat;

import com.minecraftcivilizations.specialization.Events.SkillLevelChangeEvent;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.Specialization;
import com.minecraftcivilizations.specialization.StaffTools.Debug;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static org.bukkit.ChatColor.*;
import static org.bukkit.ChatColor.GRAY;
import static org.bukkit.ChatColor.GREEN;
import static org.bukkit.ChatColor.YELLOW;

/**
 * Applies Damage, Crit Multipliers for Guardsman
 * Handles Primary Guardsman XP gain
 * Carefully balanced, hand-coded with care
 * @author Alectriciti
 */
public class GuardsmanDamage implements Listener {

    Specialization plugin;
    NamespacedKey MAX_HEALTH_KEY;
    CombatManager combatManager;

    private final double EXTRA_PENETRATION_PER_ARMOR = 0.25;

    public GuardsmanDamage(CombatManager combatManager) {
        this.combatManager = combatManager;
        this.plugin = combatManager.plugin;
        this.plugin.getServer().getPluginManager().registerEvents(this, plugin);

        //setup
        MAX_HEALTH_KEY = new NamespacedKey(plugin, "guardsman_max_health");
    }

    @EventHandler
    public void onGuardsmanLevelUp(SkillLevelChangeEvent event){
        if(event.getSkillType() == SkillType.GUARDSMAN){
            Player player = event.getPlayer();
            int new_level = event.getNewLevel();
            AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
            AttributeModifier modifier = attribute.getModifier(MAX_HEALTH_KEY);
            if(modifier != null){
                attribute.removeModifier(modifier);
            }
            modifier = new AttributeModifier(MAX_HEALTH_KEY,
                    event.getNewLevel() * 2.0, // add +2 health per level
                    AttributeModifier.Operation.ADD_NUMBER
            );
            attribute.addModifier(modifier);
        }
    }

    /**
     * Called from CombatManager
     */
    public void applyGuardsmanDamage(CustomPlayer customPlayer, EntityDamageByEntityEvent event) {
        Player damager = (Player) event.getDamager(); //damager is always a player

        // Get the player's highest skill (dmain class)
//        Skill bestSkill = customPlayer.getSkills().stream()
//                .max(Comparator.comparingDouble(Skill::getXp))
//                .orElse(null);

        Entity victim = event.getEntity();

        int lvl = customPlayer.getSkillLevel(SkillType.GUARDSMAN);
        double multiplier = Math.pow( 1.0725, lvl) - 0.25; //1.0 + ((double)lvl/10);


        //debugging
        String crit_msg = "";
        String extra_msg = "";
        String armor_msg = "";


        // Apply damage reduction for non-Guardsman players attacking mobs
//        double damageReduction = SpecializationConfig.getGuardsmanConfig().get("NON_GUARDSMAN_DAMAGE_REDUCTION", Double.class);
        double original_damage = event.getDamage();
        double new_damage = original_damage * multiplier;

        /**
         * Guardsman Extra Mob Damage Bonus
         */
//        if(victim instanceof Monster monster) {
//
//            double extra = Math.max(0, ((double) lvl - 2)) / 2.0;
//            if (extra > 0) {
//                new_damage += extra;
////                w.spawnParticle(Particle.BLOCK, monster.getEyeLocation(), (int)(extra*2), 0.33,0.33,0.33,0);
//                extra_msg = DARK_RED + " (" + DARK_RED + "+" + (Debug.formatDecimal(extra) + " 💀)");
//            }
//        }


        // Armor Bonus for EXPERT and above
        if((victim instanceof LivingEntity le) && lvl>=3) {

            double new_armor = event.getDamage(EntityDamageEvent.DamageModifier.ARMOR);
            if (new_armor < -0.1) {
                EntityEquipment equipment = le.getEquipment();
                double total_extra_penetration = 0;

                World w = victim.getWorld();
                int armor_roll = lvl - 2; // extra rolls for higher levels

                Set<EquipmentSlot> set = new HashSet<EquipmentSlot>();
                EnumMap<EquipmentSlot, Material> map = new EnumMap<>(EquipmentSlot.class);
                for (int i = 0; i < armor_roll; i++) {
                    set.add(pickRandomArmorSlot());
                }
                for (EquipmentSlot slot : set) {
                    ItemStack item = equipment.getItem(slot);
                    if (item != null) {
                        Material m = ArmorStats.getMaterialBlockType(item.getType());
                        if (m != Material.AIR) {
                            map.put(slot, m);
                            total_extra_penetration += EXTRA_PENETRATION_PER_ARMOR; //stacks damage
                        }
                    }
                }
                if (total_extra_penetration > 0) {
                    new_armor = Math.min(0, new_armor + total_extra_penetration);
//                    event.setDamage(EntityDamageEvent.DamageModifier.ARMOR, Math.min(0, new_armor));


                    armor_msg = LIGHT_PURPLE+" ("+GRAY+"👕:+"+LIGHT_PURPLE+Debug.formatDecimal(total_extra_penetration)+")";

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
                        w.spawnParticle(Particle.BLOCK, victim.getLocation().add(0, y, 0), 4, 0.125, 0.125, 0.125, 0, mat.createBlockData(), true);
                    }
                    if(sound!=null) {
                        w.playSound(victim.getLocation(), sound, SoundCategory.PLAYERS, 0.95f, 1.2f + ThreadLocalRandom.current().nextFloat(0.2f));
                    }
//                    Particle.DustOptions dust =new Particle.DustOptions(Color color, 10);
//                    w.spawnParticle(Particle.ANGRY_VILLAGER, monster.getEyeLocation(), 4, 0.5,0.6,0.5,0);
                    }
            }
        }


        double crit_multiplier = 1.0;
        if(event.isCritical()){
            new_damage *= 0.6666; //inverse of 1.5x, extra 6 for safe measure <_<
            crit_multiplier = 0.1+Math.pow( 1.07475, lvl); //slight exponent boost to crit
            new_damage *= (crit_multiplier); //apply custom crit
            crit_msg = GOLD+" ("+GRAY+"✨ "+GOLD+(Debug.formatDecimal(crit_multiplier) +"x)");
        }

        String reduction_msg = extra_msg+YELLOW+" ("+GRAY+"⚔ "+YELLOW+Debug.formatDecimal(multiplier)+"x)"+crit_msg
                +armor_msg;
                //+GREEN+" ("+GRAY+"🟰:"+GREEN+Debug.formatDecimal(crit_multiplier*multiplier)+"x)";


        //finalize damage
        event.setDamage(new_damage);

        if(Debug.isAnyoneListening("damage", true)) {
            String modifiers = "";

            for (EntityDamageEvent.DamageModifier m : EntityDamageEvent.DamageModifier.values()) {
                modifiers += "\n<gray>"+m.name()+"</gray>: "+Debug.formatDecimal(event.getDamage(m));
            }
            Debug.broadcast(
                    "damage",
                    RED+ Debug.formatDecimal(original_damage)+
                            (reduction_msg)
                            +RED+" [❤ "+Debug.formatDecimal(CombatManager.calculateTotalDamage(event))+"]",
                            "["+damager.getName()+" is GuardMan lvl "+lvl+"]"+"\n"+
                            "Attacker: "+damager.getName()+modifiers
            );
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


}
