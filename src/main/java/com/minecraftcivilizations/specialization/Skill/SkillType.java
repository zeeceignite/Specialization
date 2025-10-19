package com.minecraftcivilizations.specialization.Skill;

import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import org.apache.commons.lang3.StringUtils;
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
        return SpecializationConfig.getSkillsConfig().get(this + "_DESCRIPTION", String.class);
    }

    public Material getSkillWorkstation() {
        return SpecializationConfig.getSkillsConfig().get(this + "_WORKSTATION", Material.class);
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
        return StringUtils.capitalize(skillType.name().toLowerCase());
    }


}
