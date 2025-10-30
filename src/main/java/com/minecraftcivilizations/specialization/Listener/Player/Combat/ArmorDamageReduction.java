package com.minecraftcivilizations.specialization.Listener.Player.Combat;

import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.StaffTools.Debug;
import minecraftcivilizations.com.minecraftCivilizationsCore.Config.ConfigFile;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class ArmorDamageReduction {

    CombatManager combatManager;
    public ArmorDamageReduction(CombatManager combatManager) {
        this.combatManager = combatManager;
    }

    /**
     * Reduce Damage taken by Mobs
     */
    public void applyArmorReduction(EntityDamageByEntityEvent event) {
        // Check if the system is enabled
        if (!SpecializationConfig.getArmorDamageReductionConfig().get("ENABLED", Boolean.class)) {
            return;
        }

        // Only apply damage reduction to players being damaged by mobs
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        // Check if damage is from a mob (LivingEntity but not Player)
        // This should ONLY apply to mob damage, not PVP damage
        if (!(event.getDamager() instanceof LivingEntity) || event.getDamager() instanceof Player) {
            return;
        }

        Player player = (Player) event.getEntity();
        PlayerInventory inventory = player.getInventory();

        double totalDamageReduction = 0.0;
        
        // Check helmet
        ItemStack helmet = inventory.getHelmet();
        if (helmet != null && isArmor(helmet)) {
            double helmetReduction = calculateArmorReduction(helmet, "HELMET_BASE_REDUCTION");
            totalDamageReduction += helmetReduction;
        }
        
        // Check chestplate
        ItemStack chestplate = inventory.getChestplate();
        if (chestplate != null && isArmor(chestplate)) {
            double chestplateReduction = calculateArmorReduction(chestplate, "CHESTPLATE_BASE_REDUCTION");
            totalDamageReduction += chestplateReduction;
        }
        
        // Check leggings
        ItemStack leggings = inventory.getLeggings();
        if (leggings != null && isArmor(leggings)) {
            double leggingsReduction = calculateArmorReduction(leggings, "LEGGINGS_BASE_REDUCTION");
            totalDamageReduction += leggingsReduction;
        }
        
        // Check boots
        ItemStack boots = inventory.getBoots();
        if (boots != null && isArmor(boots)) {
            double bootsReduction = calculateArmorReduction(boots, "BOOTS_BASE_REDUCTION");
            totalDamageReduction += bootsReduction;
        }

        Entity victim = event.getEntity();

        // Apply damage reduction with configured cap - ONLY if there's actually reduction to apply
        if (totalDamageReduction > 0) {
            double maxReduction = SpecializationConfig.getArmorDamageReductionConfig().get("MAX_TOTAL_REDUCTION", Double.class);
            double originalTotalReduction = totalDamageReduction;
            totalDamageReduction = Math.min(totalDamageReduction, maxReduction);
            
            double originalDamage = event.getDamage();
            double reducedDamage = originalDamage * (1.0 - totalDamageReduction);
            event.setDamage(reducedDamage);
            if(Debug.isAnyoneListening("damage", true)) {
                Debug.broadcast(
                        "damage",
                        victim.getName()+ ChatColor.GRAY+" took "+ ChatColor.RED+"REDUCED"+ChatColor.WHITE+" damage: " + ChatColor.WHITE+
                                ((double)(Math.round(event.getDamage()*100))/100)+
                                ChatColor.RED+"[REDUCED]"+
                                (event.isCritical()? ChatColor.GREEN+" (CRIT!)":""),
                        "Original damage: "+event.getDamage()+"\n"+
                        "[Armor Reduced Damage]");
            }
        }else{
            if(Debug.isAnyoneListening("damage", true)) {
                Debug.broadcast(
                        "damage",
                        victim.getName()+" "+ ChatColor.GRAY+" was damaged: " + ChatColor.WHITE+
                                ((double)(Math.round(event.getDamage()*100))/100)+
                                (event.isCritical()? ChatColor.GREEN+" (CRIT!)":""),
                        "Original damage: "+event.getDamage()
                );
            }
        }
        // DO NOT call setDamage() when totalDamageReduction is 0 - this was causing the bug

    }
    
    /**
     * Calculate damage reduction for a specific armor piece based on material and slot
     */
    private double calculateArmorReduction(ItemStack armor, String slotReductionKey) {
        double baseReduction = SpecializationConfig.getArmorDamageReductionConfig().get(slotReductionKey, Double.class);
        double materialMultiplier = getMaterialMultiplier(armor);

        return baseReduction * materialMultiplier;
    }
    
    /**
     * Get the material multiplier for an armor piece
     */
    private double getMaterialMultiplier(ItemStack armor) {
        ConfigFile cfg = SpecializationConfig.getArmorDamageReductionConfig();
        switch (armor.getType()) {
            case DIAMOND_BOOTS:
            case DIAMOND_CHESTPLATE:
            case DIAMOND_LEGGINGS:
            case DIAMOND_HELMET:
                return cfg("DIAMOND_MULTIPLIER");
            case IRON_BOOTS:
            case IRON_CHESTPLATE:
            case IRON_LEGGINGS:
            case IRON_HELMET:
                return cfg("IRON_MULTIPLIER");
            case LEATHER_BOOTS:
            case LEATHER_CHESTPLATE:
            case LEATHER_LEGGINGS:
            case LEATHER_HELMET:
                return cfg("LEATHER_MULTIPLIER");
            case CHAINMAIL_BOOTS:
            case CHAINMAIL_CHESTPLATE:
            case CHAINMAIL_LEGGINGS:
            case CHAINMAIL_HELMET:
                return cfg("CHAINMAIL_MULTIPLIER");
            case GOLDEN_BOOTS:
            case GOLDEN_CHESTPLATE:
            case GOLDEN_LEGGINGS:
            case GOLDEN_HELMET:
                return cfg("GOLDEN_MULTIPLIER");
            case NETHERITE_BOOTS:
            case NETHERITE_CHESTPLATE:
            case NETHERITE_LEGGINGS:
            case NETHERITE_HELMET:
                return cfg("NETHERITE_MULTIPLIER");
            default:
                // Default to iron multiplier for unknown materials
                return cfg("IRON_MULTIPLIER");
        }
    }

    //Grabs cfg
    private double cfg(String key){
        return SpecializationConfig.getArmorDamageReductionConfig().get(key, Double.class);
    }
    
    /**
     * Check if an item is armor (helmet, chestplate, leggings, or boots)
     */
    private boolean isArmor(ItemStack item) {
        if (item == null) {
            return false;
        }
        
        String materialName = item.getType().name();
        return materialName.endsWith("_HELMET") || 
               materialName.endsWith("_CHESTPLATE") || 
               materialName.endsWith("_LEGGINGS") || 
               materialName.endsWith("_BOOTS");
    }
}
