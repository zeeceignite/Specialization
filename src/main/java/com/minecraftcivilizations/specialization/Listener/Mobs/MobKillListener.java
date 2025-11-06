package com.minecraftcivilizations.specialization.Listener.Mobs;

import com.google.gson.reflect.TypeToken;
import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.Skill;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.Specialization;
import com.minecraftcivilizations.specialization.StaffTools.Debug;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.List;

public class MobKillListener implements Listener {

    @EventHandler
    public void GuardsmanKillListener(EntityDeathEvent e){
        if (e.getEntity().getKiller() != null) {
            Player player = e.getEntity().getKiller();
            assert player != null;
            CustomPlayer killer = CoreUtil.getPlayer(e.getEntity().getKiller().getUniqueId());

            EntityType entity = e.getEntity().getType();

            Double xp = SpecializationConfig.getGuardsmanConfig().get(entity, Double.class);
            killer.addSkillXp(SkillType.GUARDSMAN, xp, e.getEntity().getLocation(), false);

            addCustomDrops(e);
        }
    }

    public void addCustomDrops(EntityDeathEvent e){
        List<NamespacedKey> items = SpecializationConfig.getMobDropsConfig().get(e.getEntityType(), new TypeToken<>() {});
        Material.matchMaterial(e.getEntityType().getKey().getKey());

    }

    @EventHandler
    public void GuardsmanDamageListener (EntityDamageByEntityEvent event) {
        // Check if the damager is a player
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        
        Player damager = (Player) event.getDamager();
        CustomPlayer customPlayer = CoreUtil.getPlayer(damager);
        
        // Get the player's highest skill (main class)
        Skill bestSkill = customPlayer.getSkills().stream()
                .max(Comparator.comparingDouble(Skill::getXp))
                .orElse(null);

        Entity victim = event.getEntity();


        //get effect level of guardsman

//        Specialization.getInstance().info(customPlayer.getSkillLevel(SkillType.GUARDSMAN));


        //blessed food unstackable
        //customPlayer.getSkillLevel(SkillType.GUARDSMAN))
        //weighted armor
        //

        int lvl = customPlayer.getSkillLevel(SkillType.GUARDSMAN);
        double reduction = 0.5 + ((double)lvl/10);

//        reduction = 1;
        String reduction_msg = ChatColor.GOLD+" (x"+reduction+")";

        // Apply damage reduction for non-Guardsman players attacking mobs
//        double damageReduction = SpecializationConfig.getGuardsmanConfig().get("NON_GUARDSMAN_DAMAGE_REDUCTION", Double.class);
        double currentDamage = event.getFinalDamage();
        double reducedDamage = currentDamage * reduction;
        
        event.setDamage(reducedDamage);

//        DecimalFormat df = new DecimalFormat("0.00");
//        ;
        if(Debug.isAnyoneListening("damage", true)) {
            Debug.broadcast(
                    "damage",
                    victim.getName()+ChatColor.GRAY+" took damage: " + ChatColor.RED+
                            ((double)(Math.round(reducedDamage*100))/100)+
                            (event.isCritical()? ChatColor.GREEN+" (CRIT!)":"")+
                            (reduction_msg),
                    "Original Damage: "+((double)(Math.round(currentDamage*100))/100)+"\n"+
                        "["+damager.getName()+" is GuardMan lvl "+lvl+"]"+"\n"+
                                "Attacker: "+damager.getName()
//                            ChatColor.RED+"Attacker: "+ChatColor.WHITE+damager.getName()
            );
        }
    }
}
