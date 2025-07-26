package com.minecraftcivilizations.specialization.Skill;

import com.minecraftcivilizations.specialization.Config.Config;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Material;

@AllArgsConstructor
public enum SkillLevel {
    NOVICE(0),
    APPRENTICE(1),
    JOURNEYMAN(2),
    EXPERT(3),
    MASTER(4),
    GRANDMASTER(5);

    @Getter
    private final int level;

    public static SkillLevel getSkillLevelFromInt(int skillLevel) {
        for (SkillLevel level : SkillLevel.values()) {
            if (level.level == skillLevel) {
                return level;
            }
        }
        return SkillLevel.NOVICE;
    }

    public static String getDisplayName(SkillLevel skillLevel) {
        return skillLevel.name().substring(0, 1).toUpperCase() + skillLevel.name().toLowerCase().substring(1, skillLevel.name().length());
    }

    public static String getDisplayName(int skillLevel) {
        return getDisplayName(getSkillLevelFromInt(skillLevel));
    }
}
