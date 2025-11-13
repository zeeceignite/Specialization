package com.minecraftcivilizations.specialization.Combat;

import com.minecraftcivilizations.specialization.Events.SkillLevelChangeEvent;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillLevel;
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

import static org.bukkit.event.entity.EntityDamageEvent.DamageModifier.*;

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

        double multiplier = 0.5;
        double add = 0.0;
        SkillLevel skill_level = SkillLevel.getSkillLevelFromInt(lvl);
        switch(skill_level){
            case NOVICE -> {
                multiplier = 0.25;
                add = 0.25;
            }
            case APPRENTICE -> {
                multiplier = 0.275;
                add = 0.4;
            }
            case JOURNEYMAN -> {
                multiplier = 0.3;
                add = 0.5;
            }
            case EXPERT ->  {
                multiplier = 0.4;
                add = 0.6;
            }
            case MASTER ->  {
                multiplier = 0.4;
                add = 0.8;
            } //1.4
            case GRANDMASTER -> {
                multiplier = 0.4125;
                add = 1.0;
            }
        }

//        double multiplier = Math.pow(1.084, lvl) - 0.5; //1.0 + ((double)lvl/10);
//        double multiplier = 0.5 + Math.sqrt(lvl / 5.0) * 0.654;
//        if(multiplier>1.0){
//            multiplier = 1.0;
//        }


        //debugging
        String crit_msg = "";
        String extra_msg = "";
        String armor_msg = "";


        // Apply damage reduction for non-Guardsman players attacking mobs
//        double damageReduction = SpecializationConfig.getGuardsmanConfig().get("NON_GUARDSMAN_DAMAGE_REDUCTION", Double.class);
        double original_damage = event.getDamage(BASE);

        if(victim instanceof LivingEntity le){
            if(victim instanceof Enemy || combatManager.getMobManager().isOverrideEntity(victim))
            multiplier *= 2;
        }

        double new_damage = new_damage = ((original_damage) * multiplier)+add;


//        event.setDamage(ABSORPTION, 0);

//        event.setDamage(ABSORPTION, event.getDamage(ABSORPTION)/2);

        /**
         * Guardsman Extra Mob Damage Bonus
         */
        if(victim instanceof Monster monster) {
            double extra = Math.max(0, ((double) lvl - 1)) / 2.0;
            if (event.isCritical()) {
                extra *= 1.5;
            }
            if (extra > 0) {
                new_damage += extra;
//                w.spawnParticle(Particle.BLOCK, monster.getEyeLocation(), (int)(extra*2), 0.33,0.33,0.33,0);
                extra_msg = "<dark_red> (+" + (Debug.formatDecimal(extra) + " 💀)</dark_red>");
            }
        }





        String reduction_msg = extra_msg + "<yellow> (" + "<gray>⚔</gray> " + Debug.formatDecimal(multiplier) + "x)</yellow>"
                + " <aqua>(" + "<gray>⚔</gray> +" + Debug.formatDecimal(add) + ")</aqua>"
                + armor_msg;
//                +GREEN+" ("+GRAY+"🟰:"+GREEN+Debug.formatDecimal(crit_multiplier*multiplier)+"x)";


        new_damage = Math.max(0, new_damage);






        double temp = 0;


        //finalize damage
        event.setDamage(BASE, new_damage);
//        event.setDamage(ARMOR, 0);
//        event.setDamage(RESISTANCE, 0);


        if (Debug.isAnyoneListening("damage", true)) {
            String modifiers = "";

            for (EntityDamageEvent.DamageModifier m : EntityDamageEvent.DamageModifier.values()) {
                if (event.getDamage(m) != 0)
                    modifiers += "\n<gray>" + m.name() + "</gray>: " + Debug.formatDecimal(event.getDamage(m));
            }
            Debug.broadcast(
                    "damage",
                     "<dark_red>Base Damage: <red>"+Debug.formatDecimal(original_damage) +"</red>"+
                            (reduction_msg)
                            + " <red>[❤ " + Debug.formatDecimal(event.getDamage(BASE)) + "]</red>",
                    "<gold>This output displays the calculated Guardsman Damage\nas if Vanilla Armor was being utilized</gold>\n"
                            +"[" + damager.getName() + " is GuardMan lvl " + lvl + "]" + "\n" +
                            "Attacker: " + damager.getName() + modifiers
            );
        }
    }


}
