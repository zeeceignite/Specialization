package com.minecraftcivilizations.specialization.Config;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.minecraftcivilizations.specialization.Recipe.Pair;
import com.minecraftcivilizations.specialization.Skill.SkillLevel;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.Specialization;
import lombok.Getter;
import minecraftcivilizations.com.minecraftCivilizationsCore.Config.Config;
import minecraftcivilizations.com.minecraftCivilizationsCore.Options.Field;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.inventory.*;

import java.util.HashSet;
import java.util.Set;

public class SpecializationConfig {
    @Getter
    private static Config playerConfig;
    @Getter
    private static Config skillsConfig;
    @Getter
    private static Config blockHardnessConfig;
    @Getter
    private static Config skillRequirementsConfig;
    @Getter
    private static Config unlockedRecipesConfig;
    @Getter
    private static Config defaultUnlockedRecipesConfig;
    @Getter
    private static Config xpGainFromStonecuttingConfig;
    @Getter
    private static Config xpGainFromSmeltingConfig;
    @Getter
    private static Config xpGainFromBlastingConfig;
    @Getter
    private static Config xpGainFromSmokingConfig;
    @Getter
    private static Config xpGainFromBreakingConfig;
    @Getter
    private static Config xpGainFromPlacingConfig;
    @Getter
    private static Config xpGainFromEnchantingConfig;
    @Getter
    private static Config xpGainFromCartographyConfig;
    @Getter
    private static Config xpGainFromRepairingConfig;
    @Getter
    private static minecraftcivilizations.com.minecraftCivilizationsCore.Config.Config combatConfig;
    @Getter
    private static final Set<Recipe> recipeSet = new HashSet<>();


    public static void initialize() {
        Bukkit.recipeIterator().forEachRemaining((recipe) -> {
            if (recipe instanceof Keyed keyed) {
                recipeSet.add(recipe);
            }
        });


        playerConfig = new Config(Specialization.getInstance(), "playerConfig", fields -> {
            fields.add(new Field<>("SPECIALIZATION_BONUS", Double.class, 0.3));
            fields.add(new Field<>("MULTI_CLASS_PENALTY", Double.class, 0.15));
            fields.add(new Field<>("LINEAR_DECAY_RATE", Double.class, 0.02));
            fields.add(new Field<>("CROSS_SKILL_PENALTY", Double.class, 0.25));
        });

        unlockedRecipesConfig = new Config(Specialization.getInstance(), "unlockedRecipesConfig", "The array of unlocked recipes, they don't need to repeat between levels, the ones for novice are unlocked for the next ones", fields -> {
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

        xpGainFromStonecuttingConfig = new Config(Specialization.getInstance(), "xpGainFromStonecutting", fields -> {
            for (Material inputMaterial : Material.values()) {
                if (inputMaterial.isItem() && inputMaterial != Material.AIR) {
                    Bukkit.recipeIterator().forEachRemaining((recipe) -> {
                        if (recipe instanceof StonecuttingRecipe stonecuttingRecipe) {
                            if (stonecuttingRecipe.getResult().equals(ItemStack.of(inputMaterial))) {
                                fields.add(new Field<>(inputMaterial.name(), String.class, new Gson().toJson(new Pair(SkillType.FARMER.name(), "1"))));
                            }
                        }
                    });
                }
            }
        });

        combatConfig = new minecraftcivilizations.com.minecraftCivilizationsCore.Config.Config(Specialization.getInstance(), "playerConfig", fields -> {
            fields.add(new Field<>("CROSSBOW_BASE_VELOCITY", Double.class, 1.6));
            fields.add(new Field<>("CROSSBOW_BASE_PIERCING_VELOCITY", Double.class, 1.3));
            fields.add(new Field<>("CROSSBOW_BASE_MULTISHOT_VELOCITY", Double.class, 2.5));
            fields.add(new Field<>("CROSSBOW_BASE_QUICKCHARGE_VELOCITY", Double.class, 1.3));
        });

        xpGainFromRepairingConfig = new Config(Specialization.getInstance(), "xpGainFromRepairing", fields -> {
            for (Material inputMaterial : Material.values()) {
                if (inputMaterial.isItem() && inputMaterial != Material.AIR && inputMaterial.getMaxDurability() > 0) {
                    fields.add(new Field<>(inputMaterial.name(), String.class, new Gson().toJson(new Pair(SkillType.LIBRARIAN.name(), "1"))));
                }
            }
        });

        xpGainFromBlastingConfig = new Config(Specialization.getInstance(), "xpGainFromBlasting", fields -> {
            for (Material inputMaterial : Material.values()) {
                if (inputMaterial.isItem() && inputMaterial != Material.AIR) {
                    Bukkit.recipeIterator().forEachRemaining((recipe) -> {
                        if (recipe instanceof BlastingRecipe blastingRecipe) {
                            if (blastingRecipe.getResult().equals(ItemStack.of(inputMaterial))) {
                                fields.add(new Field<>(inputMaterial.name(), String.class, new Gson().toJson(new Pair(SkillType.FARMER.name(), "1"))));
                            }
                        }
                    });
                }
            }
        });

        xpGainFromSmeltingConfig = new Config(Specialization.getInstance(), "xpGainFromSmelting", fields -> {
            for (Material inputMaterial : Material.values()) {
                if (inputMaterial.isItem() && inputMaterial != Material.AIR) {
                    Bukkit.recipeIterator().forEachRemaining((recipe) -> {
                        if (recipe instanceof FurnaceRecipe furnaceRecipe) {
                            if (furnaceRecipe.getResult().equals(ItemStack.of(inputMaterial))) {
                                fields.add(new Field<>(inputMaterial.name(), String.class, new Gson().toJson(new Pair(SkillType.FARMER.name(), "1"))));
                            }
                        }
                    });
                }
            }
        });

        xpGainFromSmokingConfig = new Config(Specialization.getInstance(), "xpGainFromSmoking", fields -> {
            for (Material inputMaterial : Material.values()) {
                if (inputMaterial.isItem() && inputMaterial != Material.AIR) {
                    Bukkit.recipeIterator().forEachRemaining((recipe) -> {
                        if (recipe instanceof SmokingRecipe smokingRecipe) {
                            if (smokingRecipe.getResult().equals(ItemStack.of(inputMaterial))) {
                                fields.add(new Field<>(inputMaterial.name(), String.class, new Gson().toJson(new Pair(SkillType.FARMER.name(), "1"))));
                            }
                        }
                    });
                }
            }
        });

        xpGainFromBreakingConfig = new Config(Specialization.getInstance(), "xpGainFromBreaking", fields -> {
            for (Material inputMaterial : Material.values()) {
                if (inputMaterial.isBlock()) {
                    fields.add(new Field<>(inputMaterial.name(), String.class, new Gson().toJson(new Pair(SkillType.FARMER.name(), "1"))));
                }
            }
        });

        xpGainFromPlacingConfig = new Config(Specialization.getInstance(), "xpGainFromPlacing", fields -> {
            for (Material inputMaterial : Material.values()) {
                if (inputMaterial.isBlock()) {
                    fields.add(new Field<>(inputMaterial.name(), String.class, new Gson().toJson(new Pair(SkillType.FARMER.name(), "1"))));
                }
            }
        });

        defaultUnlockedRecipesConfig = new Config(Specialization.getInstance(), "defaultUnlockedRecipesConfig", fields -> {
            Set<Pair> strings = new HashSet<>();
            Bukkit.recipeIterator().forEachRemaining((recipe) -> {
                if (recipe instanceof Keyed keyed) {
                    strings.add(new Pair(keyed.getKey().getNamespace(), keyed.getKey().getKey()));
                }
            });
            fields.add(new Field<>("DEFAULT_UNLOCKED_RECIPES", String.class, new Gson().toJson(strings, new TypeToken<Set<Pair>>() {
            }.getType())));
        });

        blockHardnessConfig = new Config(Specialization.getInstance(), "blockHardnessConfig", fields -> {
            for (Material material : Material.values()) {
                if (material.isBlock() && !material.isAir()) {
                    fields.add(new Field<>(material.name(), Double.class, 1D));
                }
            }
        });

        skillsConfig = new Config(Specialization.getInstance(), "skillsConfig", fields -> {
            for (SkillType skillType : SkillType.values()) {
                fields.add(new Field<>(skillType.name() + "_WORKSTATION", String.class, Material.COMPOSTER.name()));
                fields.add(new Field<>(skillType.name() + "_DESCRIPTION", String.class, "Description"));
            }
        });

        skillRequirementsConfig = new Config(Specialization.getInstance(), "skillRequirementsConfig", "the number represents the percentage of total xp in this skill needed to level it up each level", fields -> {
            for (SkillType skillType : SkillType.values()) {
                for (SkillLevel skillLevel : SkillLevel.values()) {
                    fields.add(new Field<>(skillType.name() + "_" + skillLevel.name() + "_REQUIREMENT", Double.class, 0D));
                }
            }
        });
    }

    public static void reload() {
        playerConfig.reload();
        unlockedRecipesConfig.reload();
        xpGainFromStonecuttingConfig.reload();
        defaultUnlockedRecipesConfig.reload();
        blockHardnessConfig.reload();
        skillsConfig.reload();
        skillRequirementsConfig.reload();
        combatConfig.reload();
    }


}
