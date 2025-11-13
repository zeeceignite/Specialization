package com.minecraftcivilizations.specialization.Combat.Mobs;

import lombok.Getter;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;

import java.util.HashMap;
import java.util.Map;

/**
 * a light-weight random selector for overriding vanilla mobs to sprinkle in some variety
 * @author Alectriciti
 */
public class MobOverrideRule {


    @Getter
    private final int baseChance;

    //Map to chance
    private Map<EntityType, Integer> overrides = new HashMap<>();

    @Getter
    private Biome biome;

    private int total_roll = -1;

    private boolean spawn_in_packs = false;

    /**
     * Establishes a base chance of no override taking place
     * Build upon this with override(entity_type, chance)
     */
    public MobOverrideRule(int base_chance){
        baseChance = base_chance;
    }

    public MobOverrideRule add(EntityType override, int chance){
        overrides.put(override, chance);
        return this;
    }


    public MobOverrideRule spawnInPacks(){
        this.spawn_in_packs = true;
        return this;
    }

    public boolean doesSpawnInPacks(){
        return spawn_in_packs;
    }


    /**
     * Sets the biome for this ruleset
     */
    public MobOverrideRule biome(){
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
