package com.minecraftcivilizations.specialization.Listener.Player.Combat;

import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.StaffTools.Debug;
import minecraftcivilizations.com.minecraftCivilizationsCore.Config.ConfigFile;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import static org.bukkit.event.entity.EntityDamageEvent.DamageModifier.*;
import static org.bukkit.ChatColor.*;

public class ArmorDamageReduction {

    CombatManager combatManager;
    boolean enabled;

    public ArmorDamageReduction(CombatManager combatManager) {
        this.combatManager = combatManager;

        enabled = SpecializationConfig.getArmorDamageReductionConfig().get("ENABLED", Boolean.class);
    }

    /**
     * Reduce Damage taken by Mobs
     * TARGET: 16 hits with iron, 32 hits with diamond
     */
    public void applyArmorReduction(LivingEntity victim, EntityDamageByEntityEvent event) {
//        if (!(event.getDamager() instanceof LivingEntity attacker)) return;

        // Check if the system is enabled
//        if (!enabled) {
//            return;
//        }

//        int lvl = 0;
//
//        boolean is_player = false;
//        if(victim instanceof Player player) {
//            // Utilize Guardsman Armor buff
//            is_player = true;
//            CustomPlayer customPlayer = CustomPlayer.getCustomPlayer(player);
//            lvl = customPlayer.getSkillLevel(SkillType.GUARDSMAN);
//        }


//        if(lvl>=5){
//            return; //full effectiveness
//        }
        ArmorStats stats = ArmorStats.getArmorStats(victim.getEquipment());
        double original_base = event.getDamage(BASE);
        double original_armor = event.getDamage(ARMOR);
        double armor_ceiling = 24;

        double armor = stats.getArmor();
        double toughness = stats.getToughness();


        double ARMOR_REDUCTION = original_base * (armor / armor_ceiling);
        double TOUGHNESS_REDUCTION = Math.max (0, toughness / 16);
//        double TOUGHNESS_REDUCTION = 0; //(HARD SUBTRACT)

        double TOTAL_REDUCTION;
        TOTAL_REDUCTION = Math.min(original_base, (ARMOR_REDUCTION) + (TOUGHNESS_REDUCTION));


        //inverse finally
        event.setDamage(ARMOR, -TOTAL_REDUCTION);

        //Blocking
        //scaled armor reduction effectiveness according to guardsman level

        if(Debug.isAnyoneListening("armor", true)) {
            String modifiers = "";

            for (EntityDamageEvent.DamageModifier m : EntityDamageEvent.DamageModifier.values()) {
                if(event.getDamage(m)!=0)
                modifiers += "\n<gray>"+m.name()+"</gray>: "+Debug.formatDecimal(event.getDamage(m));
            }

            Debug.broadcast(
                    "armor",
                    //WHITE+victim.getName()+" "+*
                    DARK_RED+"Reduction: "+RED+ Debug.formatDecimal(event.getDamage())+
//                            (WHITE+" ["+BLUE+"🅱: "+Debug.formatDecimal(original_armor)+"]")+
                            WHITE+" ["+BLUE+"👕: "+Debug.formatDecimal(-ARMOR_REDUCTION)+"x"+WHITE+"]"+
                            ((stats.getToughness()>0)?(WHITE+" ["+GRAY+"🪨: "+Debug.formatDecimal(-TOUGHNESS_REDUCTION)+WHITE+"]"):"")+
                            (WHITE+" ["+GREEN+"🚫: "+Debug.formatDecimal(TOTAL_REDUCTION)+"]")+
                            (event.isCritical()? GREEN+" (CRIT!)":"")+
                            RED+" [❤ "+Debug.formatDecimal(CombatManager.calculateTotalDamage(event))+"]"
                    ,
                    "<blue>ARMOR REDUCTION:</blue> "+Debug.formatDecimal(ARMOR_REDUCTION)+"\n"
                            +"<light_purple>TOUGHNESS REDUCTION:</light_purple> "+Debug.formatDecimal(TOUGHNESS_REDUCTION)+"\n"
                            +"\nOriginal damage: "+Debug.formatDecimal(event.getDamage())
                            +modifiers
            );
        }

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

        switch (item.getType()) {
            case DIAMOND_BOOTS:
            case DIAMOND_CHESTPLATE:
            case DIAMOND_LEGGINGS:
            case DIAMOND_HELMET:
            case IRON_BOOTS:
            case IRON_CHESTPLATE:
            case IRON_LEGGINGS:
            case IRON_HELMET:
            case LEATHER_BOOTS:
            case LEATHER_CHESTPLATE:
            case LEATHER_LEGGINGS:
            case LEATHER_HELMET:
            case CHAINMAIL_BOOTS:
            case CHAINMAIL_CHESTPLATE:
            case CHAINMAIL_LEGGINGS:
            case CHAINMAIL_HELMET:
            case GOLDEN_BOOTS:
            case GOLDEN_CHESTPLATE:
            case GOLDEN_LEGGINGS:
            case GOLDEN_HELMET:
            case NETHERITE_BOOTS:
            case NETHERITE_CHESTPLATE:
            case NETHERITE_LEGGINGS:
            case NETHERITE_HELMET:
                return true;
            default:
                return false;
        }
    }
}
