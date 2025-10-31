package com.minecraftcivilizations.specialization.Listener.Player.Combat;

import com.minecraftcivilizations.specialization.Events.SkillLevelChangeEvent;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.Skill;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.Specialization;
import com.minecraftcivilizations.specialization.StaffTools.Debug;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Comparator;
import java.util.EnumMap;

import static org.bukkit.ChatColor.*;
import static org.bukkit.ChatColor.GRAY;
import static org.bukkit.ChatColor.GREEN;
import static org.bukkit.ChatColor.YELLOW;
import static org.bukkit.entity.EntityType.*;

/**
 * Applies Damage, Crit Multipliers for Guardsman
 * Handles Primary Guardsman XP gain
 * Carefully balanced
 * @author Alectriciti
 */
public class GuardsmanDamage implements Listener {

    Specialization plugin;
    NamespacedKey MAX_HEALTH_KEY;
    CombatManager combatManager;

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
        double multiplier = Math.pow( 1.125, lvl) - 0.5; //1.0 + ((double)lvl/10);


        //debugging
        String crit_msg = "";
        String extra_msg = "";


        // Apply damage reduction for non-Guardsman players attacking mobs
//        double damageReduction = SpecializationConfig.getGuardsmanConfig().get("NON_GUARDSMAN_DAMAGE_REDUCTION", Double.class);
        double original_damage = event.getDamage();
        double new_damage = original_damage * multiplier;

        /**
         * Guardsman Extra Mob Damage Bonus
         */
        if(victim instanceof Monster monster){
            double extra = Math.max(0, ((double)lvl-2))/2.0 ;
            if(extra>=0) {
                new_damage += extra;
                extra_msg = DARK_RED + " (" + DARK_RED + "+" + (Debug.formatDecimal(extra) + " 💀)");
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
                +GREEN+" ("+GRAY+"🟰:"+GREEN+Debug.formatDecimal(crit_multiplier*multiplier)+"x)";


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




}
