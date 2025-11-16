package com.minecraftcivilizations.specialization.Combat.Mobs;

import com.minecraftcivilizations.specialization.StaffTools.Debug;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.EntityType;

import java.util.*;

/**
 * a light-weight random selector for overriding vanilla mobs to sprinkle in some variety
 * @author Alectriciti
 */
public class MobOverrideRule {

    @Setter
    @Getter //this is the UNIVERSAL base chance for all MobOverrideRules. Raising this value makes Vanilla spawning logic more common
    private static int defaultBaseChance = 100;

    @Getter
    private final int chance;

    @Getter
    private final EnumSet<EntityType> replaceTypes;

    //Map to chance
    private final Map<MobVariation, Integer> variation_mapping = new HashMap<>();

    private int total_roll = -1;

    /**
     * Establishes a base chance of no override taking place
     * Build upon this with override(entity_type, chance)
     * @param rule_chance this is the chance to roll this rule. this is rolled using MobOverrideRule.globalBaseChance
     */
    public MobOverrideRule(int rule_chance, EntityType...replace_types){
        this.replaceTypes = EnumSet.noneOf(EntityType.class);
        Collections.addAll(this.replaceTypes, replace_types);
        this.chance = rule_chance;
        MobManager.getInstance().registerRule(this);
    }

    public MobOverrideRule addVariation(MobVariation variation, int variation_chance){
        variation_mapping.put(variation, variation_chance);
        return this;
    }


    /**
     * Adds a variation using the global base chance
     */
    public MobOverrideRule addVariation(MobVariation variation){
        variation_mapping.put(variation, defaultBaseChance);
        return this;
    }

//    Set<Biome> biomes = new HashSet<Biome>();
//    private boolean restricted_to_biomes = false;
//
//    public MobOverrideRule whitelistOnlyBiomes(Biome...biomes){
//        restricted_to_biomes = Collections.addAll(this.biomes, biomes);
//        return this;
//    }

//    public boolean supportsBiome(Biome biome){
//        if(!restricted_to_biomes) return true;
//        return this.biomes.contains(biome);
//    }



    public MobVariation rollVariation() {
        calculateTotalRoll();
//        if (total_roll == -1) {
//            calculateTotalRoll();
//        }

        int roll = (int) (Math.random() * total_roll);
        int current = chance;

        if (roll < chance) {
            return null; // no override triggered
        }

        for (Map.Entry<MobVariation, Integer> entry : variation_mapping.entrySet()) {
            current += entry.getValue();
            if (roll < current) {
                return entry.getKey();
            }
        }

        return null; // fallback
    }

    private void calculateTotalRoll() {
        total_roll = chance;
        for(int i : variation_mapping.values()){
            total_roll += i;
        }
        Debug.broadcast("mobrule", "<green>calculated total roll:</green> "+total_roll);
    }

    private boolean does_spawn_in_packs = false;

    public MobOverrideRule spawnInPacks(){
        this.does_spawn_in_packs = true;
        return this;
    }

    public boolean doesSpawnInPacks() {
        return does_spawn_in_packs;
    }
}
