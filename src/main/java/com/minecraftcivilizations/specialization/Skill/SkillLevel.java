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
}
