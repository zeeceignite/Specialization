package com.minecraftcivilizations.specialization.Recipe;

import com.minecraftcivilizations.specialization.Specialization;
import minecraftcivilizations.com.minecraftCivilizationsCore.Item.CustomItem;
import minecraftcivilizations.com.minecraftCivilizationsCore.Item.CustomItemRegistry;
import org.bukkit.*;
import org.bukkit.inventory.*;

import java.util.ArrayList;
import java.util.List;

public class Recipes {

    public static void init() {
        registerCustomItems();
        unregisterRecipes();
        registerRecipes(false);
    }

    private static void registerCustomItems() {
        // Your custom items – unchanged
    }

    public static void unregisterRecipes() {
        Bukkit.removeRecipe(NamespacedKey.minecraft("rail"));
    }

    public static void registerRecipes(boolean reloading) {
        int successCount = 0;
        int skippedMissingCount = 0;
        int skippedDuplicateCount = 0;
        List<String> failedExceptions = new ArrayList<>();

        // ----- CUSTOM ITEM RECIPES -----
        for (NamespacedKey key : CustomItemRegistry.getItems().keySet()) {
            CustomItem customItem = CustomItemRegistry.getItem(key);
            if (customItem == null) {
                skippedMissingCount++;
                continue;
            }

            if (recipeExists(key, customItem.getItem())) {
                skippedDuplicateCount++;
                continue;
            }

            try {
                Bukkit.addRecipe(new ShapelessRecipe(key, customItem.getItem()), true);
                successCount++;
            } catch (Exception e) {
                failedExceptions.add(key.getKey() + " (" + e.getMessage() + ")");
            }
        }

        // ----- CUSTOM "RAIL" RECIPE -----
        NamespacedKey railKey = new NamespacedKey(Specialization.getInstance(), "rail_alt");
        ShapedRecipe rail = new ShapedRecipe(railKey, new ItemStack(Material.RAIL, 64));
        rail.shape("I I", "ISI", "I I");
        rail.setIngredient('I', Material.IRON_INGOT);
        rail.setIngredient('S', Material.STICK);
        if (!recipeExists(railKey, rail.getResult())) {
            try {
                Bukkit.addRecipe(rail, true);
                successCount++;
            } catch (Exception e) {
                failedExceptions.add("rail_alt (" + e.getMessage() + ")");
            }
        } else skippedDuplicateCount++;

        // ----- EXTRA RECIPES -----
        successCount += addNetherRecipes(failedExceptions, skippedDuplicateCount);
        addUnobtainableRecipes(failedExceptions, skippedDuplicateCount);

        // ----- FINAL LOG -----
        Bukkit.getLogger().info("[Recipes] Registration complete. Total successes: " + successCount);
        if (skippedMissingCount > 0)
            Bukkit.getLogger().info("[Recipes] Skipped " + skippedMissingCount + " recipes: missing items.");
        if (skippedDuplicateCount > 0)
            Bukkit.getLogger().info("[Recipes] Skipped " + skippedDuplicateCount + " recipes: duplicates.");
        if (!failedExceptions.isEmpty()) {
            Bukkit.getLogger().warning("[Recipes] Failed recipes due to exceptions (" + failedExceptions.size() + "):");
            failedExceptions.forEach(f -> Bukkit.getLogger().warning(" - " + f));
        }
    }

    private static boolean recipeExists(NamespacedKey key, ItemStack result) {
        return Bukkit.getRecipesFor(result).stream()
                .filter(r -> r instanceof Keyed)
                .map(r -> (Keyed) r)
                .anyMatch(r -> r.getKey().equals(key));
    }

    public static int addNetherRecipes(List<String> failedExceptions, int skippedDuplicateCount) {
        int count = 0;

        NamespacedKey netheriteKey = new NamespacedKey(Specialization.getInstance(), "netherite_upgrade");
        ShapelessRecipe netheriteUpgrade = new ShapelessRecipe(netheriteKey,
                new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
        netheriteUpgrade.addIngredient(1, Material.NETHERITE_INGOT);
        netheriteUpgrade.addIngredient(6, Material.DIAMOND);
        netheriteUpgrade.addIngredient(1, Material.NETHER_WART_BLOCK);
        if (!recipeExists(netheriteKey, netheriteUpgrade.getResult())) {
            try { Bukkit.addRecipe(netheriteUpgrade, true); count++; }
            catch (Exception e) { failedExceptions.add("netherite_upgrade (" + e.getMessage() + ")"); }
        } else skippedDuplicateCount++;

        NamespacedKey blazeRodKey = new NamespacedKey(Specialization.getInstance(), "blaze_rod");
        ShapelessRecipe blazeRod = new ShapelessRecipe(blazeRodKey, new ItemStack(Material.BLAZE_ROD));
        blazeRod.addIngredient(1, Material.GOLD_INGOT);
        blazeRod.addIngredient(3, Material.GUNPOWDER);
        blazeRod.addIngredient(1, Material.CRIMSON_NYLIUM);
        blazeRod.addIngredient(1, Material.WARPED_NYLIUM);
        if (!recipeExists(blazeRodKey, blazeRod.getResult())) {
            try { Bukkit.addRecipe(blazeRod, true); count++; }
            catch (Exception e) { failedExceptions.add("blaze_rod (" + e.getMessage() + ")"); }
        } else skippedDuplicateCount++;

        NamespacedKey netherWartKey = new NamespacedKey(Specialization.getInstance(), "nether_wart");
        ShapedRecipe netherWart = new ShapedRecipe(netherWartKey, new ItemStack(Material.NETHER_WART));
        netherWart.shape(" E ", "DDD", " B ");
        netherWart.setIngredient('E', Material.BEETROOT);
        netherWart.setIngredient('D', Material.COARSE_DIRT);
        netherWart.setIngredient('B', Material.BLAZE_POWDER);
        if (!recipeExists(netherWartKey, netherWart.getResult())) {
            try { Bukkit.addRecipe(netherWart, true); count++; }
            catch (Exception e) { failedExceptions.add("nether_wart (" + e.getMessage() + ")"); }
        } else skippedDuplicateCount++;

        return count;
    }

    public static void addUnobtainableRecipes(List<String> failedExceptions, int skippedDuplicateCount) {
        NamespacedKey catEggKey = new NamespacedKey(Specialization.getInstance(), "cat_spawn_egg");
        ShapedRecipe catEgg = new ShapedRecipe(catEggKey, new ItemStack(Material.CAT_SPAWN_EGG));
        catEgg.shape("FFF", " E ", "FDF");
        catEgg.setIngredient('F', Material.TROPICAL_FISH);
        catEgg.setIngredient('E', Material.EGG);
        catEgg.setIngredient('D', Material.DIAMOND);
        if (!recipeExists(catEggKey, catEgg.getResult())) {
            try { Bukkit.addRecipe(catEgg, true); }
            catch (Exception e) { failedExceptions.add("cat_spawn_egg (" + e.getMessage() + ")"); }
        } else skippedDuplicateCount++;

        NamespacedKey bellKey = new NamespacedKey(Specialization.getInstance(), "bell");
        ShapedRecipe bell = new ShapedRecipe(bellKey, new ItemStack(Material.BELL));
        bell.shape(" W ", "GGG", "GGG");
        bell.setIngredient('W', new RecipeChoice.MaterialChoice(Tag.PLANKS));
        bell.setIngredient('G', Material.GOLD_INGOT);
        if (!recipeExists(bellKey, bell.getResult())) {
            try { Bukkit.addRecipe(bell, true); }
            catch (Exception e) { failedExceptions.add("bell (" + e.getMessage() + ")"); }
        } else skippedDuplicateCount++;
    }
}
