package com.minecraftcivilizations.specialization.Config;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.minecraftcivilizations.specialization.Recipe.Pair;
import com.minecraftcivilizations.specialization.Skill.SkillLevel;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.Specialization;
import lombok.Getter;
import minecraftcivilizations.com.minecraftCivilizationsCore.Options.Field;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;

import java.util.*;

public class Config {
    @Getter
    private static minecraftcivilizations.com.minecraftCivilizationsCore.Config.Config playerConfig;
    @Getter
    private static minecraftcivilizations.com.minecraftCivilizationsCore.Config.Config skillsConfig;
    @Getter
    private static minecraftcivilizations.com.minecraftCivilizationsCore.Config.Config breakBlockConfig;
    @Getter
    private static minecraftcivilizations.com.minecraftCivilizationsCore.Config.Config blockHardnessConfig;
    @Getter
    private static minecraftcivilizations.com.minecraftCivilizationsCore.Config.Config placeBlockConfig;
    @Getter
    private static minecraftcivilizations.com.minecraftCivilizationsCore.Config.Config skillRequirementsConfig;
    @Getter
    private static minecraftcivilizations.com.minecraftCivilizationsCore.Config.Config unlockedRecipesConfig;
    @Getter
    private static minecraftcivilizations.com.minecraftCivilizationsCore.Config.Config defaultUnlockedRecipesConfig;

    public static void initialize() {
        playerConfig = new minecraftcivilizations.com.minecraftCivilizationsCore.Config.Config(Specialization.getInstance(), "playerConfig", fields -> {
            fields.add(new Field<>("SPECIALIZATION_BONUS", Double.class, 0.3));
            fields.add(new Field<>("MULTI_CLASS_PENALTY", Double.class, 0.15));
            fields.add(new Field<>("LINEAR_DECAY_RATE", Double.class, 0.02));
            fields.add(new Field<>("CROSS_SKILL_PENALTY", Double.class, 0.25));
        });

        breakBlockConfig = new minecraftcivilizations.com.minecraftCivilizationsCore.Config.Config(Specialization.getInstance(), "breakBlockXpGainConfig", fields -> {
            for (Material material : Material.values()) {
                if (material.isBlock() && !material.isAir()) {
                    fields.add(new Field<>(material.name(), Double.class, 6D));
                }
            }
        });

        unlockedRecipesConfig = new minecraftcivilizations.com.minecraftCivilizationsCore.Config.Config(Specialization.getInstance(), "unlockedRecipesConfig", "The array of unlocked recipes, they don't need to repeat between levels, the ones for novice are unlocked for the next ones", fields -> {
            for (SkillType skillType : SkillType.values()) {
                for (SkillLevel skillLevel : SkillLevel.values()) {
                    Set<Pair> strings = new HashSet<>();
                    Bukkit.recipeIterator().forEachRemaining((recipe) -> {
                        if (recipe instanceof Keyed keyed) {
                            strings.add(new Pair(keyed.getKey().getNamespace(), keyed.getKey().getKey()));
                        }
                    });
                    fields.add(new Field<>(skillType.name() + "_" + skillLevel.name(), String.class, new Gson().toJson(strings, new TypeToken<Set<Pair>>() {}.getType())));
                }
            }
        });

        defaultUnlockedRecipesConfig = new minecraftcivilizations.com.minecraftCivilizationsCore.Config.Config(Specialization.getInstance(), "defaultUnlockedRecipesConfig", fields -> {
            Set<Pair> strings = new HashSet<>();
            Bukkit.recipeIterator().forEachRemaining((recipe) -> {
                if (recipe instanceof Keyed keyed) {
                    strings.add(new Pair(keyed.getKey().getNamespace(), keyed.getKey().getKey()));
                }
            });
            fields.add(new Field<>("DEFAULT_UNLOCKED_RECIPES", String.class, new Gson().toJson(strings, new TypeToken<Set<Pair>>() {
            }.getType())));
        });

        blockHardnessConfig = new minecraftcivilizations.com.minecraftCivilizationsCore.Config.Config(Specialization.getInstance(), "blockHardnessConfig", fields -> {
            for (Material material : Material.values()) {
                if (material.isBlock() && !material.isAir()) {
                    fields.add(new Field<>(material.name(), Double.class, 1D));
                }
            }
        });

        placeBlockConfig = new minecraftcivilizations.com.minecraftCivilizationsCore.Config.Config(Specialization.getInstance(), "placeBlockXpGainConfig", fields -> {
            for (Material material : Material.values()) {
                if (material.isBlock() && !material.isAir()) {
                    fields.add(new Field<>(material.name(), Double.class, 1D));
                }
            }
        });


        skillsConfig = new minecraftcivilizations.com.minecraftCivilizationsCore.Config.Config(Specialization.getInstance(), "skillsConfig", fields -> {
            for (SkillType skillType : SkillType.values()) {
                fields.add(new Field<>(skillType.name() + "_WORKSTATION", String.class, Material.COMPOSTER.name()));
                fields.add(new Field<>(skillType.name() + "_DESCRIPTION", String.class, "Description"));
            }
        });

        skillRequirementsConfig = new minecraftcivilizations.com.minecraftCivilizationsCore.Config.Config(Specialization.getInstance(), "skillRequirementsConfig", "the number represents the percentage of total xp in this skill needed to level it up each level", fields -> {
            for (SkillType skillType : SkillType.values()) {
                for (SkillLevel skillLevel : SkillLevel.values()) {
                    fields.add(new Field<>(skillType.name() + "_" + skillLevel.name() + "_REQUIREMENT", Double.class, 0D));
                }
            }
        });
    }

    public static void reload() {
        playerConfig.reload();
        breakBlockConfig.reload();
        unlockedRecipesConfig.reload();
        defaultUnlockedRecipesConfig.reload();
        blockHardnessConfig.reload();
        placeBlockConfig.reload();
        skillsConfig.reload();
        skillRequirementsConfig.reload();
    }


}
