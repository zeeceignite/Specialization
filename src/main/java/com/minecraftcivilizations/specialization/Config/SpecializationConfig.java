package com.minecraftcivilizations.specialization.Config;

import minecraftcivilizations.com.minecraftCivilizationsCore.Config.ConfigFile;
import minecraftcivilizations.com.minecraftCivilizationsCore.Options.Pair;
import com.minecraftcivilizations.specialization.Skill.SkillLevel;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.Specialization;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.*;

import java.util.HashSet;
import java.util.Set;

public class SpecializationConfig {
    @Getter
    private static ConfigFile playerConfig;
    @Getter
    private static ConfigFile skillsConfig;
    @Getter
    private static ConfigFile blockHardnessConfig;
    @Getter
    private static ConfigFile skillRequirementsConfig;
    @Getter
    private static ConfigFile unlockedRecipesConfig;
    @Getter
    private static ConfigFile defaultUnlockedRecipesConfig;
    @Getter
    private static ConfigFile xpGainFromStonecuttingConfig;
    @Getter
    private static ConfigFile xpGainFromSmeltingConfig;
    @Getter
    private static ConfigFile xpGainFromBlastingConfig;
    @Getter
    private static ConfigFile xpGainFromSmokingConfig;
    @Getter
    private static ConfigFile xpGainFromBreakingConfig;
    @Getter
    private static ConfigFile xpGainFromPlacingConfig;
    @Getter
    private static ConfigFile xpGainFromEnchantingConfig;
    @Getter
    private static ConfigFile xpGainFromCartographyConfig;
    @Getter
    private static ConfigFile xpGainFromRepairingConfig;
    @Getter
    private static ConfigFile combatConfig;
    @Getter
    private static ConfigFile chatConfig;
    @Getter
    private static ConfigFile mobConfig;


    public static void initialize() {
        playerConfig = new ConfigFile(Specialization.getInstance(), "playerConfig", null, fields -> {
            fields.add(new Pair<>("SPECIALIZATION_BONUS", 0.3));
            fields.add(new Pair<>("MULTI_CLASS_PENALTY", 0.15));
            fields.add(new Pair<>("LINEAR_DECAY_RATE", 0.02));
            fields.add(new Pair<>("CROSS_SKILL_PENALTY", 0.25));
        });

        unlockedRecipesConfig = new ConfigFile(Specialization.getInstance(), "unlockedRecipesConfig", "The array of unlocked recipes, they don't need to repeat between levels, the ones for novice are unlocked for the next ones", fields -> {
            for (SkillType skillType : SkillType.values()) {
                for (SkillLevel skillLevel : SkillLevel.values()) {
                    Set<NamespacedKey> namespacedKeys = new HashSet<>();
                    Bukkit.recipeIterator().forEachRemaining((recipe) -> {
                        if (recipe instanceof Keyed keyed) {
                            namespacedKeys.add(keyed.getKey());
                        }
                    });
                    fields.add(new Pair<>(skillType + "_" + skillLevel, namespacedKeys));
                }
            }
        });

        xpGainFromStonecuttingConfig = new ConfigFile(Specialization.getInstance(), "xpGainFromStonecutting", null, fields -> {
            for (Material inputMaterial : Material.values()) {
                if (inputMaterial.isItem() && inputMaterial != Material.AIR) {
                    Bukkit.recipeIterator().forEachRemaining((recipe) -> {
                        if (recipe instanceof StonecuttingRecipe stonecuttingRecipe) {
                            if (stonecuttingRecipe.getResult().equals(ItemStack.of(inputMaterial))) {
                                fields.add(new Pair<>(inputMaterial, new Pair<>(SkillType.FARMER, "1")));
                            }
                        }
                    });
                }
            }
        });

        combatConfig = new ConfigFile(Specialization.getInstance(), "combatConfig", null, fields -> {
            fields.add(new Pair<>("CROSSBOW_BASE_VELOCITY", 1.6));
            fields.add(new Pair<>("CROSSBOW_BASE_PIERCING_VELOCITY", 1.3));
            fields.add(new Pair<>("CROSSBOW_BASE_MULTISHOT_VELOCITY", 2.5));
            fields.add(new Pair<>("CROSSBOW_BASE_QUICKCHARGE_VELOCITY", 1.3));
        });

        mobConfig = new ConfigFile(Specialization.getInstance(), "mobConfig", null, fields -> {
            fields.add(new Pair<>("DAYTIME_MOB_DAMAGE_MULTIPLIER", 4.0));
            fields.add(new Pair<>("NIGHTTIME_MOB_DAMAGE_MULTIPLIER", 10.0));
            fields.add(new Pair<>("NIGHT_GUARDSMAN_MOB_DAMAGE_PERCENT_REDUCTION", 30));
            fields.add(new Pair<>("DAYTIME_SPEED_BUFF", .25));
            fields.add(new Pair<>("NIGHTTIME_SPEED_BUFF", .35));
        });

        xpGainFromRepairingConfig = new ConfigFile(Specialization.getInstance(), "xpGainFromRepairing", null, fields -> {
            for (Material inputMaterial : Material.values()) {
                if (inputMaterial.isItem() && inputMaterial != Material.AIR && inputMaterial.getMaxDurability() > 0) {
                    fields.add(new Pair<>(inputMaterial, new Pair<>(SkillType.LIBRARIAN, "1")));
                }
            }
        });

        xpGainFromBlastingConfig = new ConfigFile(Specialization.getInstance(), "xpGainFromBlasting", null, fields -> {
            for (Material inputMaterial : Material.values()) {
                if (inputMaterial.isItem() && inputMaterial != Material.AIR) {
                    Bukkit.recipeIterator().forEachRemaining((recipe) -> {
                        if (recipe instanceof BlastingRecipe blastingRecipe) {
                            if (blastingRecipe.getResult().equals(ItemStack.of(inputMaterial))) {
                                fields.add(new Pair<>(inputMaterial, new Pair<>(SkillType.FARMER, "1")));
                            }
                        }
                    });
                }
            }
        });

        xpGainFromSmeltingConfig = new ConfigFile(Specialization.getInstance(), "xpGainFromSmelting", null, fields -> {
            for (Material inputMaterial : Material.values()) {
                if (inputMaterial.isItem() && inputMaterial != Material.AIR) {
                    Bukkit.recipeIterator().forEachRemaining((recipe) -> {
                        if (recipe instanceof FurnaceRecipe furnaceRecipe) {
                            if (furnaceRecipe.getResult().equals(ItemStack.of(inputMaterial))) {
                                fields.add(new Pair<>(inputMaterial, new Pair<>(SkillType.FARMER, "1")));
                            }
                        }
                    });
                }
            }
        });

        xpGainFromSmokingConfig = new ConfigFile(Specialization.getInstance(), "xpGainFromSmoking", null, fields -> {
            for (Material inputMaterial : Material.values()) {
                if (inputMaterial.isItem() && inputMaterial != Material.AIR) {
                    Bukkit.recipeIterator().forEachRemaining((recipe) -> {
                        if (recipe instanceof SmokingRecipe smokingRecipe) {
                            if (smokingRecipe.getResult().equals(ItemStack.of(inputMaterial))) {
                                fields.add(new Pair<>(inputMaterial, new Pair<>(SkillType.FARMER, "1")));
                            }
                        }
                    });
                }
            }
        });

        xpGainFromBreakingConfig = new ConfigFile(Specialization.getInstance(), "xpGainFromBreaking", null, fields -> {
            for (Material inputMaterial : Material.values()) {
                if (inputMaterial.isBlock()) {
                    fields.add(new Pair<>(inputMaterial, new Pair<>(SkillType.FARMER, "1")));
                }
            }
        });

        xpGainFromPlacingConfig = new ConfigFile(Specialization.getInstance(), "xpGainFromPlacing", null, fields -> {
            for (Material inputMaterial : Material.values()) {
                if (inputMaterial.isBlock()) {
                    fields.add(new Pair<>(inputMaterial, new Pair<>(SkillType.FARMER, "1")));
                }
            }
        });

        defaultUnlockedRecipesConfig = new ConfigFile(Specialization.getInstance(), "defaultUnlockedRecipesConfig", null, fields ->
            Bukkit.recipeIterator().forEachRemaining((recipe) -> {
                if (recipe instanceof Keyed keyed) {
                    fields.add(new Pair<>("DEFAULT_UNLOCKED_RECIPES", keyed.getKey()));
                }
        }));

        blockHardnessConfig = new ConfigFile(Specialization.getInstance(), "blockHardnessConfig", null, fields -> {
            for (Material material : Material.values()) {
                if (material.isBlock() && !material.isAir()) {
                    fields.add(new Pair<>(material, 1D));
                }
            }
        });

        skillsConfig = new ConfigFile(Specialization.getInstance(), "skillsConfig", null, fields -> {
            for (SkillType skillType : SkillType.values()) {
                fields.add(new Pair<>(skillType + "_WORKSTATION", Material.COMPOSTER));
                fields.add(new Pair<>(skillType + "_DESCRIPTION", "Description"));
            }
        });

        skillRequirementsConfig = new ConfigFile(Specialization.getInstance(), "skillRequirementsConfig", "the number represents the percentage of total xp in this skill needed to level it up each level", fields -> {
            for (SkillType skillType : SkillType.values()) {
                for (SkillLevel skillLevel : SkillLevel.values()) {
                    fields.add(new Pair<>(skillType + "_" + skillLevel + "_REQUIREMENT", 0D));
                }
            }
        });

        chatConfig = new ConfigFile(Specialization.getInstance(), "chatConfig", null, fields -> {
            fields.add(new Pair<>("CHAT_RADIUS", 32.0));
            fields.add(new Pair<>("DEFAULT_FORMAT", "%s > %s"));
            fields.add(new Pair<>("ANNOUNCEMENT_FORMAT", "<aqua>[Announcement]<gray> %s"));
            fields.add(new Pair<>("ANNOUNCEMENT_PREFIX", "#"));
        });
    }
}
