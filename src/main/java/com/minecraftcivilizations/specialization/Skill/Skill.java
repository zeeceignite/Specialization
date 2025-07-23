package com.minecraftcivilizations.specialization.Skill;

import com.minecraftcivilizations.specialization.Config.Config;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;

@AllArgsConstructor
public class Skill {
    @Getter
    @Setter
    private SkillType skillType;
    @Setter
    private SkillLevel skillLevel = SkillLevel.NOVICE;
    @Getter
    private double xp;
    @Getter
    private long lastUpdate;


    public static double getXPNeededForLevel(int level) {
        return (25 * Math.pow(level, 2) + (5 * level) + (200*Math.pow(2.2, level)));
    }

    public static double mapValue(double x, double in_min, double in_max, double out_min, double out_max) {
        return out_min + (x - in_min) * (out_max - out_min) / (in_max - in_min);
    }

    public SkillLevel getSkillLevel() {
        skillLevel = SkillLevel.getSkillLevelFromInt(getLevelFromXP(this.xp));
        return skillLevel;
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

    public void addXp(double xp) {
        this.xp += xp;
        this.lastUpdate = System.currentTimeMillis();
    }

    public static String getDisplayName(SkillLevel skillLevel) {
        return skillLevel.name().substring(0, 1).toUpperCase() + skillLevel.name().toLowerCase().substring(1, skillLevel.name().length());
    }

    public static String getDisplayName(SkillType skillType) {
        return skillType.name().substring(0, 1).toUpperCase() + skillType.name().toLowerCase().substring(1, skillType.name().length());
    }

}
