package com.minecraftcivilizations.specialization.Listener.Player.Combat;

import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Specialization;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class ArmorDamageReductionListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
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
        
        Specialization.logger.info("=== ARMOR DAMAGE REDUCTION DEBUG ===");
        Specialization.logger.info("Player: " + player.getName() + " damaged by: " + event.getDamager().getType());
        Specialization.logger.info("Original damage: " + event.getDamage());
        
        double totalDamageReduction = 0.0;
        
        // Check helmet
        ItemStack helmet = inventory.getHelmet();
        if (helmet != null && isArmor(helmet)) {
            double helmetReduction = calculateArmorReduction(helmet, "HELMET_BASE_REDUCTION");
            totalDamageReduction += helmetReduction;
            Specialization.logger.info("Helmet (" + helmet.getType() + "): " + (helmetReduction * 100) + "% reduction");
        } else {
            Specialization.logger.info("No helmet equipped");
        }
        
        // Check chestplate
        ItemStack chestplate = inventory.getChestplate();
        if (chestplate != null && isArmor(chestplate)) {
            double chestplateReduction = calculateArmorReduction(chestplate, "CHESTPLATE_BASE_REDUCTION");
            totalDamageReduction += chestplateReduction;
            Specialization.logger.info("Chestplate (" + chestplate.getType() + "): " + (chestplateReduction * 100) + "% reduction");
        } else {
            Specialization.logger.info("No chestplate equipped");
        }
        
        // Check leggings
        ItemStack leggings = inventory.getLeggings();
        if (leggings != null && isArmor(leggings)) {
            double leggingsReduction = calculateArmorReduction(leggings, "LEGGINGS_BASE_REDUCTION");
            totalDamageReduction += leggingsReduction;
            Specialization.logger.info("Leggings (" + leggings.getType() + "): " + (leggingsReduction * 100) + "% reduction");
        } else {
            Specialization.logger.info("No leggings equipped");
        }
        
        // Check boots
        ItemStack boots = inventory.getBoots();
        if (boots != null && isArmor(boots)) {
            double bootsReduction = calculateArmorReduction(boots, "BOOTS_BASE_REDUCTION");
            totalDamageReduction += bootsReduction;
            Specialization.logger.info("Boots (" + boots.getType() + "): " + (bootsReduction * 100) + "% reduction");
        } else {
            Specialization.logger.info("No boots equipped");
        }
        
        Specialization.logger.info("Total calculated reduction: " + (totalDamageReduction * 100) + "%");
        
        // Apply damage reduction with configured cap - ONLY if there's actually reduction to apply
        if (totalDamageReduction > 0) {
            double maxReduction = SpecializationConfig.getArmorDamageReductionConfig().get("MAX_TOTAL_REDUCTION", Double.class);
            double originalTotalReduction = totalDamageReduction;
            totalDamageReduction = Math.min(totalDamageReduction, maxReduction);
            
            if (originalTotalReduction > maxReduction) {
                Specialization.logger.info("Reduction capped at: " + (maxReduction * 100) + "% (was " + (originalTotalReduction * 100) + "%)");
            }
            
            double originalDamage = event.getDamage();
            double reducedDamage = originalDamage * (1.0 - totalDamageReduction);
            event.setDamage(reducedDamage);
            
            Specialization.logger.info("Final damage: " + reducedDamage + " (reduced by " + (originalDamage - reducedDamage) + ")");
        } else {
            Specialization.logger.info("No armor equipped - no damage reduction applied");
            // DO NOT call setDamage() when totalDamageReduction is 0 - this was causing the bug
        }
        
        Specialization.logger.info("=== END ARMOR DAMAGE REDUCTION DEBUG ===");
    }
    
    /**
     * Calculate damage reduction for a specific armor piece based on material and slot
     */
    private double calculateArmorReduction(ItemStack armor, String slotReductionKey) {
        double baseReduction = SpecializationConfig.getArmorDamageReductionConfig().get(slotReductionKey, Double.class);
        double materialMultiplier = getMaterialMultiplier(armor);
        
        double finalReduction = baseReduction * materialMultiplier;
        
        Specialization.logger.info("  - Base reduction (" + slotReductionKey + "): " + (baseReduction * 100) + "%");
        Specialization.logger.info("  - Material multiplier (" + armor.getType() + "): " + materialMultiplier + "x");
        Specialization.logger.info("  - Final piece reduction: " + (finalReduction * 100) + "%");
        
        return finalReduction;
    }
    
    /**
     * Get the material multiplier for an armor piece
     */
    private double getMaterialMultiplier(ItemStack armor) {
        String materialName = armor.getType().name();
        
        if (materialName.startsWith("LEATHER_")) {
            return SpecializationConfig.getArmorDamageReductionConfig().get("LEATHER_MULTIPLIER", Double.class);
        } else if (materialName.startsWith("CHAINMAIL_")) {
            return SpecializationConfig.getArmorDamageReductionConfig().get("CHAINMAIL_MULTIPLIER", Double.class);
        } else if (materialName.startsWith("IRON_")) {
            return SpecializationConfig.getArmorDamageReductionConfig().get("IRON_MULTIPLIER", Double.class);
        } else if (materialName.startsWith("DIAMOND_")) {
            return SpecializationConfig.getArmorDamageReductionConfig().get("DIAMOND_MULTIPLIER", Double.class);
        } else if (materialName.startsWith("GOLDEN_")) {
            return SpecializationConfig.getArmorDamageReductionConfig().get("GOLDEN_MULTIPLIER", Double.class);
        } else if (materialName.startsWith("NETHERITE_")) {
            return SpecializationConfig.getArmorDamageReductionConfig().get("NETHERITE_MULTIPLIER", Double.class);
        }
        
        // Default to iron multiplier for unknown materials
        return SpecializationConfig.getArmorDamageReductionConfig().get("IRON_MULTIPLIER", Double.class);
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
