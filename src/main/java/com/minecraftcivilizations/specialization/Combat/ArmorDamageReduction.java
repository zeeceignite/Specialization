package com.minecraftcivilizations.specialization.Combat;

import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.StaffTools.Debug;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import com.minecraftcivilizations.specialization.util.PlayerUtil;
import minecraftcivilizations.com.minecraftCivilizationsCore.Config.ConfigFile;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import static org.bukkit.event.entity.EntityDamageEvent.DamageModifier.*;

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

        double original_damage = CombatManager.calculateTotalDamage(event);

        double armor = stats.getArmor();
        double toughness = stats.getToughness();

        // GM should be 1
        // Noob should be 1.5

        double armor_redux_factor = 2;
        double toughness_redux_factor = 8;
        if (event.getDamager() instanceof Player) {
            armor_redux_factor = 2;
        }else{
            //Attacker is mob
            if(event.getEntity() instanceof Player pvictim){
                armor_redux_factor = 1.25; //default armor reduction against mobs
                CustomPlayer cp = CoreUtil.getPlayer(pvictim);
                int guardsman_level = cp.getSkillLevel(SkillType.GUARDSMAN);
                armor_ceiling = 26;
                armor_redux_factor -= ((double)guardsman_level)*0.04;
                toughness_redux_factor = 12;
            }
        }
        // ARMOR (SCALING) REDUCTION
        double ARMOR_REDUCTION = (armor / armor_ceiling) / armor_redux_factor;
        // TOUGHNESS (LINEAR) REDUCTION
        double TOUGHNESS_REDUCTION = Math.max (0, (toughness / toughness_redux_factor)); // Absolute damage reduction

        //FORMULA II
//        double TOUGHNESS_REDUCTION_LINEAR = Math.max (0, (toughness / 8)); // Absolute damage reduction
//        double TOUGHNESS_REDUCTION_SCALAR = Math.max (0, original_base / (8+(toughness/4))); // Relative damage reduction
//        double TOUGHNESS_REDUCTION = TOUGHNESS_REDUCTION_LINEAR + TOUGHNESS_REDUCTION_SCALAR;
//
//        double TOTAL_REDUCTION;
//        TOTAL_REDUCTION = Math.min(original_base, (original_base * ARMOR_REDUCTION) + (TOUGHNESS_REDUCTION));
        // vanilla-like toughness: scales with incoming damage and toughness (diminishing returns)
//        double TOUGHNESS_REDUCTION = Math.max(0.0, original_base / (2.0 + toughness / 4.0));
        double TOTAL_REDUCTION = Math.min(original_base, (original_base * ARMOR_REDUCTION) + TOUGHNESS_REDUCTION);


        //inverse finally
        event.setDamage(ARMOR, -TOTAL_REDUCTION);


        double MAGIC_REDUCTION = 0;
        if(event.isApplicable(MAGIC)) {
            //we apply magic (protection enchantment) to armor reduction
            double original_magic =  - event.getDamage(MAGIC); //inverted
            MAGIC_REDUCTION = event.getDamage(MAGIC)*0.5;
            event.setDamage(MAGIC, MAGIC_REDUCTION);
        }
        //Blocking
        //scaled armor reduction effectiveness according to guardsman level

        if(Debug.isAnyoneListening("damage", true)) {
            if(event.getDamager() instanceof Player px) {
                String modifiers = "";

                for (EntityDamageEvent.DamageModifier m : EntityDamageEvent.DamageModifier.values()) {
                    if (event.getDamage(m) != 0)
                        modifiers += "\n<gray>" + m.name() + "</gray>: " + Debug.formatDecimal(event.getDamage(m));
                }

                Debug.message(px,
                        "armor",
                        //WHITE+victim.getName()+" "+*
                        "<dark_red>Armor: </dark_red><red>" + Debug.formatDecimal(original_base) +
//                            (WHITE+" ["+BLUE+"🅱: "+Debug.formatDecimal(original_armor)+"]")+
                                " <blue>[👕: " + Debug.formatDecimal(ARMOR_REDUCTION) + "x]</blue>" +
                                ((stats.getToughness() > 0) ? (" <gray>[🪨: -" + Debug.formatDecimal(TOUGHNESS_REDUCTION) + "]</gray>") : "") +
                                (" <green>[🚫: " + Debug.formatDecimal(TOTAL_REDUCTION) + "]</green>") +
//                            (event.isCritical()? GREEN+" (CRIT!)":"")+
                                " [❤ " + Debug.formatDecimal(CombatManager.calculateTotalDamage(event)) + "]</red>"
                        ,
                        "<gray>Original Armor Reduction: <dark_blue>" + Debug.formatDecimal(-original_armor) + "</dark_blue>\n"
                                + "<gray>New Armor Reduction: <blue>" + Debug.formatDecimal(TOTAL_REDUCTION) + "</blue>\n"
                                + "Vanilla Damage would have been <dark_red>" + Debug.formatDecimal(original_damage) + "</dark_red>\n"
                                + "Specialization Custom Damage is <red>" + Debug.formatDecimal(CombatManager.calculateTotalDamage(event)) + "</red>\n"
                                + "<blue>ARMOR REDUCTION:</blue> " + Debug.formatDecimal(ARMOR_REDUCTION) + "\n"
                                + "<light_purple>TOUGHNESS REDUCTION:</light_purple> " + Debug.formatDecimal(TOUGHNESS_REDUCTION) + "\n"
                                + "<yellow>MAGIC REDUCTION:</yellow> " + Debug.formatDecimal(MAGIC_REDUCTION) + "\n"
                                + "\nOriginal damage: " + Debug.formatDecimal(event.getDamage())
                                + modifiers
                );
            }
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
