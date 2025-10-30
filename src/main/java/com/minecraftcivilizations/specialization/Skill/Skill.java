package com.minecraftcivilizations.specialization.Skill;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

@AllArgsConstructor
public class Skill {

    @Getter
    @Setter
    private SkillType skillType;
    @Getter
    private double xp;
    @Getter
    private long lastUpdate;



    /**
     * This should be pre-cached per level <= max_level
     */
    public static double getXPNeededForLevel(int level) {
        return Math.floor(1.8 * (25 * Math.pow(level, 2) + (5 * level) + (200 * Math.pow(2.45, level))) - 300);
    }

    public static double mapValue(double x, double in_min, double in_max, double out_min, double out_max) {
        // Handle division by zero case when in_max equals in_min
        if (in_max == in_min) {
            return out_min; // Return minimum output value when input range is zero
        }
        return out_min + (x - in_min) * (out_max - out_min) / (in_max - in_min);
    }


    public void applyXp(Player player, double appliedXp, boolean allowNegative) {
        if(appliedXp!=0) {
            if (!allowNegative) {
                this.xp += Math.max(appliedXp, 0); //prevents unintentional negative xp gain
            } else {
                this.xp += appliedXp; //can potentially subtract xp
                if (this.xp < 0) {
                    this.xp = 0; //ensures xp does not get set below zero
                }
            }
            //instant serialize hot-patch until redesign
            // TODO PDC-xp-hotfix
            //  player.getPersistentDataContainer().set(skilltype_key_map.get(skillType), PersistentDataType.INTEGER, (int) appliedXp);
        }
        this.lastUpdate = System.currentTimeMillis();
    }



    // PDC-xp-hotfix static Map<SkillType, NamespacedKey> skilltype_key_map = new HashMap<SkillType, NamespacedKey>();
    /**
     * PDC-xp-hotfix TODO do not remove until xp loss bug has been resolved
     * This assigns a NamespacedKey for each SkillType
     * Runs on startup
     */
    /*
    public static void InitializeSkillKeys(Specialization plugin) {
        for(SkillType type : SkillType.values()){
            NamespacedKey key = new NamespacedKey(plugin, "skill."+type.name().toLowerCase()+".xp");
            skilltype_key_map.put(type, key);
        }
    }
     */
}
