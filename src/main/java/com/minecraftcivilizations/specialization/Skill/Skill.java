package com.minecraftcivilizations.specialization.Skill;

import com.minecraftcivilizations.specialization.Config.Config;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;

import static com.minecraftcivilizations.specialization.Skill.SkillType.getLevelFromXP;

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
        return Math.floor(25 * Math.pow(level, 2) + (5 * level) + (200*Math.pow(2.2, level)));
    }

    public static double mapValue(double x, double in_min, double in_max, double out_min, double out_max) {
        return out_min + (x - in_min) * (out_max - out_min) / (in_max - in_min);
    }

    public void addXp(double xp) {
        this.xp += xp;
        this.lastUpdate = System.currentTimeMillis();
    }

}
