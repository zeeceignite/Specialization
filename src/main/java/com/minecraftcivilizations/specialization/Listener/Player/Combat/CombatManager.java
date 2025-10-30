package com.minecraftcivilizations.specialization.Listener.Player.Combat;

import com.google.gson.reflect.TypeToken;
import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Events.SkillLevelChangeEvent;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.Skill;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.Specialization;
import com.minecraftcivilizations.specialization.StaffTools.Debug;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import static org.bukkit.entity.EntityType.*;
import static org.bukkit.ChatColor.*;

import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Handles anything and everything related to combat, including mob damage
 */
public class CombatManager implements Listener {

    NamespacedKey MAX_HEALTH_KEY;
    public CombatManager(Specialization specialization) {
        MAX_HEALTH_KEY = new NamespacedKey(specialization, "guardsman_max_health");
        specialization.getServer().getPluginManager().registerEvents(this, specialization);
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

    public Set<EntityType> xp_mobs = Set.of(
            ZOMBIE, DROWNED, HUSK,
            SKELETON, STRAY,
            CREEPER,
            SPIDER,
            PHANTOM,
            PILLAGER,
            ILLUSIONER,
            RAVAGER,
            SLIME,
            SILVERFISH,


            //NETHER
            PIGLIN_BRUTE,
            MAGMA_CUBE,
            BLAZE,
            GHAST,

            //END
            ENDERMAN,
            ENDERMITE,
            SHULKER,
            ENDER_DRAGON
    );

    @EventHandler
    public void GuardsmanDamageListener (EntityDamageByEntityEvent event) {
        // Check if the damager is a player
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        Player damager = (Player) event.getDamager();
        CustomPlayer customPlayer = CoreUtil.getPlayer(damager);


        // Get the player's highest skill (dmain class)
        Skill bestSkill = customPlayer.getSkills().stream()
                .max(Comparator.comparingDouble(Skill::getXp))
                .orElse(null);

        Entity victim = event.getEntity();

        int lvl = customPlayer.getSkillLevel(SkillType.GUARDSMAN);
        double multiplier = Math.pow( 1.05, lvl); //1.0 + ((double)lvl/10);

        // Apply damage reduction for non-Guardsman players attacking mobs
//        double damageReduction = SpecializationConfig.getGuardsmanConfig().get("NON_GUARDSMAN_DAMAGE_REDUCTION", Double.class);
        double currentDamage = event.getFinalDamage();
        double reducedDamage = currentDamage * multiplier;

        DecimalFormat format = new DecimalFormat("#.##");
        String crit_msg = "";
        double crit_multiplier = 1.0;
        if(event.isCritical()){
            reducedDamage *= 0.6666;
            crit_multiplier = 0.1+Math.pow( 1.05475, lvl);
            reducedDamage *= (crit_multiplier);
//            reducedDamage *= crit_multiplier;
            crit_msg = GOLD+" ("+GRAY+"CRIT: "+GOLD+(format.format(crit_multiplier) +"x)");
        }


        String reduction_msg = YELLOW+" ("+GRAY+"SKILL:"+YELLOW+format.format(multiplier)+"x)"+crit_msg
                +GREEN+" ("+GRAY+"TOTAL:"+GREEN+format.format(crit_multiplier*multiplier)+"x)";

        event.setDamage(reducedDamage);

        if(xp_mobs.contains(victim.getType())){
            LivingEntity le = (LivingEntity) victim;
//            if(mob.isAggressive()) {
                double xp = event.getDamage();
                if(xp > le.getHealth()){
                    xp = le.getHealth();
                }
//                xp *= 0.5;
//            Debug.broadcast("damage", "xp should be given: "+xp);
                customPlayer.addSkillXp(SkillType.GUARDSMAN, (int) xp);
//            }
        }

//        if(Debug.isAnyoneListening("damage", true)) {
            Debug.broadcast(
                    "damage",
                    "⚔ " + ChatColor.RED+
                            ((double)(Math.round(reducedDamage*100))/100)+
                            (reduction_msg),
                    "Original Damage: "+((double)(Math.round(currentDamage*100))/100)+"\n"+
                            "["+damager.getName()+" is GuardMan lvl "+lvl+"]"+"\n"+
                            "Attacker: "+damager.getName()
//                            ChatColor.RED+"Attacker: "+ChatColor.WHITE+damager.getName()
            );
//        }
    }

    /**
     * Temporary max health for mobs
     */
    @EventHandler
    public void onMobSpawn(CreatureSpawnEvent event){
        AttributeInstance attribute = event.getEntity().getAttribute(Attribute.MAX_HEALTH);
        double max_health = attribute.getValue()*2;
        attribute.setBaseValue(max_health);
        event.getEntity().setHealth(max_health);
    }

    /**
     * Custom Mob Drops
     */
    @EventHandler
    public void GuardsmanKillListener(EntityDeathEvent e){
        if (e.getEntity().getKiller() != null) {
            Player player = e.getEntity().getKiller();
            assert player != null;
//            CustomPlayer killer = CoreUtil.getPlayer(e.getEntity().getKiller().getUniqueId());
//            EntityType entity = e.getEntity().getType();

            addCustomDrops(e);
        }
    }

    public void addCustomDrops(EntityDeathEvent e){
        List<NamespacedKey> items = SpecializationConfig.getMobDropsConfig().get(e.getEntityType(), new TypeToken<>() {});
        Material.matchMaterial(e.getEntityType().getKey().getKey());
    }

}
