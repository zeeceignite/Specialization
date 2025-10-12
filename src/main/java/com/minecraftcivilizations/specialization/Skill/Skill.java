package com.minecraftcivilizations.specialization.Skill;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
public class Skill {
    @Getter
    @Setter
    private SkillType skillType;
    @Getter
    private double xp;
    @Getter
    private long lastUpdate;


    public static double getXPNeededForLevel(int level) {
        return Math.floor(1.8 * (25 * Math.pow(level, 2) + (5 * level) + (200*Math.pow(2.45, level))) - 300);
    }

    public static double mapValue(double x, double in_min, double in_max, double out_min, double out_max) {
        // Handle division by zero case when in_max equals in_min
        if (in_max == in_min) {
            return out_min; // Return minimum output value when input range is zero
        }
        return out_min + (x - in_min) * (out_max - out_min) / (in_max - in_min);
    }

    public void xp(double xp) {
        this.xp += xp;
        this.lastUpdate = System.currentTimeMillis();
    }

}
