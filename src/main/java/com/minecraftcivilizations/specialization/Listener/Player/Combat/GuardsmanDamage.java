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
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Comparator;
import java.util.EnumMap;

import static org.bukkit.ChatColor.*;
import static org.bukkit.ChatColor.GRAY;
import static org.bukkit.ChatColor.GREEN;
import static org.bukkit.ChatColor.YELLOW;
import static org.bukkit.entity.EntityType.*;

/**
 * Applies Damage, Crit Multipliers for Guardsman
 * Handles primary Guardsman XP gain
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
        initializeMobXpMappings();
    }

    EnumMap<EntityType, Double> mob_xp_mappings = new EnumMap<>(EntityType.class);

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

    private void putXpFor(double xp, EntityType...entities){
        for(EntityType e : entities){
            mob_xp_mappings.put(e, xp);
        }
    }

    /**
     * Called from CombatManager
     */
    public void applyGuardsmanDamage(EntityDamageByEntityEvent event) {
        Player damager = (Player) event.getDamager(); //damager is always a player
        CustomPlayer customPlayer = CoreUtil.getPlayer(damager);

        // Get the player's highest skill (dmain class)
//        Skill bestSkill = customPlayer.getSkills().stream()
//                .max(Comparator.comparingDouble(Skill::getXp))
//                .orElse(null);

        Entity victim = event.getEntity();

        int lvl = customPlayer.getSkillLevel(SkillType.GUARDSMAN);
        double multiplier = Math.pow( 1.075, lvl); //1.0 + ((double)lvl/10);

        // Apply damage reduction for non-Guardsman players attacking mobs
//        double damageReduction = SpecializationConfig.getGuardsmanConfig().get("NON_GUARDSMAN_DAMAGE_REDUCTION", Double.class);
        double currentDamage = event.getFinalDamage();
        double reducedDamage = currentDamage * multiplier;

        String crit_msg = "";
        double crit_multiplier = 1.0;
        if(event.isCritical()){
            reducedDamage *= 0.6666;
            crit_multiplier = 0.1+Math.pow( 1.03475, lvl);
            reducedDamage *= (crit_multiplier);
//            reducedDamage *= crit_multiplier;
            crit_msg = GOLD+" ("+GRAY+"CRIT: "+GOLD+(Debug.formatDecimal(crit_multiplier) +"x)");
        }

        String reduction_msg = YELLOW+" ("+GRAY+"SKILL:"+YELLOW+Debug.formatDecimal(multiplier)+"x)"+crit_msg
                +GREEN+" ("+GRAY+"TOTAL:"+GREEN+Debug.formatDecimal(crit_multiplier*multiplier)+"x)";


        //finalize damage
        event.setDamage(reducedDamage);

            if(mob_xp_mappings.containsKey(victim.getType())){
                LivingEntity le = (LivingEntity) victim;
                double xp = event.getDamage();
                if(xp > le.getHealth()){
                    xp = le.getHealth();
                }
                customPlayer.addSkillXp(SkillType.GUARDSMAN, (int) (xp* mob_xp_mappings.get(victim.getType())));
            }

        if(Debug.isAnyoneListening("damage", true)) {
            Debug.broadcast(
                    "damage",
                    "⚔ " + ChatColor.RED+
                            ((double)(Math.round(reducedDamage*100))/100)+
                            (reduction_msg),
                    "Original Damage: "+((double)(Math.round(currentDamage*100))/100)+"\n"+
                            "["+damager.getName()+" is GuardMan lvl "+lvl+"]"+"\n"+
                            "Attacker: "+damager.getName()
            );
        }
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


}
