package com.minecraftcivilizations.specialization.Combat.Mobs;


import com.minecraftcivilizations.specialization.util.MathUtils;
import lombok.Getter;

/**
 *
 * @author alectriciti
 */
public class MobStats {

    //defaults for all hostile mobs
    @Getter
    private double damageMultiplier = 1.5;
    @Getter
    private double healthMultiplier = 1.0;
    @Getter
    private double speedMultiplierDay = 1.0;
    @Getter
    private double speedMultiplierNight = 1.25;
    @Getter
    private double waterSpeedMultiplierDay = 1.0;
    @Getter
    private double waterSpeedMultiplierNight = 1.2;
    @Getter
    private double sizeSmallest = 1.0;
    @Getter
    private double sizeLargest = 1.0;

    private boolean scale_enabled = false;

    public MobStats(){}

    public MobStats(double dmg_multiplier){
        this.damageMultiplier = dmg_multiplier;
    }

    public MobStats(double dmg_multiplier, double health_multiplier){
        this.damageMultiplier = dmg_multiplier;
        this.healthMultiplier = health_multiplier;
    }

    public MobStats damage(double multiplier){
        this.damageMultiplier = multiplier;
        return this;
    }

    public MobStats health(double multiplier){
        this.healthMultiplier = multiplier;
        return this;
    }

    public MobStats speed(double day_multiplier, double night_multiplier){
        this.speedMultiplierNight = night_multiplier;
        this.speedMultiplierDay = day_multiplier;
        return this;
    }


    public MobStats waterspeed(double day_multiplier, double night_multiplier){
        this.waterSpeedMultiplierNight = night_multiplier;
        this.waterSpeedMultiplierDay = day_multiplier;
        return this;
    }

    public MobStats size(double smallest, double largest) {
        this.scale_enabled = true;
        this.sizeSmallest = smallest;
        this.sizeLargest = largest;
        return this;
    }

    public double getRandomScale() {
        if(scale_enabled) {
            return MathUtils.random(sizeSmallest, sizeLargest);
        }
        return sizeLargest;
    }
}