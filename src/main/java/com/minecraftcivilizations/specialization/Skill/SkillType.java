package com.minecraftcivilizations.specialization.Skill;

import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import org.bukkit.Material;

import static com.minecraftcivilizations.specialization.Skill.Skill.getXPNeededForLevel;

public enum SkillType {

    FARMER,
    BUILDER,
    MINER,
    HEALER,
    LIBRARIAN,
    GUARDSMAN,
    BLACKSMITH;

    public String getSkillDescription() {
        return SpecializationConfig.getSkillsConfig().getString(this.name() + "_DESCRIPTION");
    }

    public Material getSkillWorkstation() {
        return Material.valueOf(SpecializationConfig.getSkillsConfig().getString(this.name() + "_WORKSTATION"));
    }

    public static int getLevelFromXP(double xp) {
        int level = 0;
        while (xp > getXPNeededForLevel(level)) {
            if (xp < getXPNeededForLevel(level+1)) {
                return level;
            }
            level++;
        }
        return level;
    }

    public static String getDisplayName(SkillType skillType) {
        return skillType.name().substring(0, 1).toUpperCase() + skillType.name().toLowerCase().substring(1, skillType.name().length());
    }


}
