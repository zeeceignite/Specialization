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
        return (50 * Math.pow(level, 2) + (5 * level) + (100*Math.pow(2, level)));
    }

    public static double mapValue(double x, double in_min, double in_max, double out_min, double out_max) {
        return out_min + (x - in_min) * (out_max - out_min) / (in_max - in_min);
    }

    public SkillLevel getSkillLevel() {
        skillLevel = SkillLevel.values()[getLevelFromXP(this.xp)];
        return skillLevel;
    }

    public static int getLevelFromXP(double xp) {
        int level = 0;
        while (getXPNeededForLevel(level) < xp) {
            level++;
        }
        return level;
    }

    public void addXp(double xp) {
        this.xp += xp;
        this.lastUpdate = System.currentTimeMillis();
    }

    public static String getDisplayName(SkillLevel skillLevel) {
        return skillLevel.name().toLowerCase().replace(skillLevel.name().substring(0, 1).toLowerCase(), skillLevel.name().substring(0, 1).toUpperCase());
    }

    public static String getDisplayName(SkillType skillType) {
        return skillType.name().toLowerCase().replace(skillType.name().substring(0, 1).toLowerCase(), skillType.name().substring(0, 1).toUpperCase());
    }

}
