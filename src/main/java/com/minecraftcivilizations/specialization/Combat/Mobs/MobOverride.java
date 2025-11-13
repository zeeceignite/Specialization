package com.minecraftcivilizations.specialization.Combat.Mobs;

import lombok.Getter;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;

import java.util.HashMap;
import java.util.Map;

public class MobOverride {


    @Getter
    private final int baseChance;

    private Map<EntityType, Integer> overrides = new HashMap<>();

    @Getter
    private Biome biome;

    private int total_roll = -1;

    /**
     * Establishes a base chance of no override taking place
     * Build upon this with override(entity_type, chance)
     */
    public MobOverride(int base_chance){
        baseChance = base_chance;
    }

    public MobOverride add(EntityType override, int chance){
        overrides.put(override, chance);
        return this;
    }


    /**
     * Sets the biome for this ruleset
     */
    public MobOverride biome(){
        this.biome = biome;
        return this;
    }


    public EntityType rollType() {
        if (total_roll == -1) {
            calculateTotalRoll();
        }

        int roll = (int) (Math.random() * total_roll);
        int current = baseChance;

        if (roll < baseChance) {
            return null; // no override triggered
        }

        for (Map.Entry<EntityType, Integer> entry : overrides.entrySet()) {
            current += entry.getValue();
            if (roll < current) {
                return entry.getKey();
            }
        }

        return null; // fallback
    }

    private void calculateTotalRoll() {
        total_roll = baseChance;
        for(int i : overrides.values()){
            total_roll += i;
        }
    }

}
