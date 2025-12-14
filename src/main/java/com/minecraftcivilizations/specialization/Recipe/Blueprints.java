package com.minecraftcivilizations.specialization.Recipe;

import com.minecraftcivilizations.specialization.Specialization;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;

import java.util.*;

public class Blueprints implements Listener {

    private static final Map<Material, String> TIER_MATERIALS = new LinkedHashMap<>();
    private static final Map<String, String> TIER_DISPLAY_NAMES = new LinkedHashMap<>();

    static {
        TIER_MATERIALS.put(Material.IRON_INGOT, "Iron");
        TIER_MATERIALS.put(Material.GOLD_INGOT, "Golden");
        TIER_MATERIALS.put(Material.DIAMOND, "Diamond");
        TIER_MATERIALS.put(Material.NETHERITE_SCRAP, "Netherite");

        TIER_DISPLAY_NAMES.put("Iron", "Iron");
        TIER_DISPLAY_NAMES.put("Golden", "Gold");
        TIER_DISPLAY_NAMES.put("Diamond", "Diamond");
        TIER_DISPLAY_NAMES.put("Netherite", "Netherite");
    }
    private static final Map<String, Material> ARMOR_PLATE_MATERIALS = new LinkedHashMap<>();

    static {
        ARMOR_PLATE_MATERIALS.put("Iron", Material.IRON_INGOT);
        ARMOR_PLATE_MATERIALS.put("Golden", Material.GOLD_INGOT);
        ARMOR_PLATE_MATERIALS.put("Diamond", Material.DIAMOND);
        ARMOR_PLATE_MATERIALS.put("Netherite", Material.NETHERITE_INGOT);
    }
    private static final Map<String, List<ArmorPair>> ARMOR_CRAFTING = new LinkedHashMap<>();

    static {
        ARMOR_CRAFTING.put("Iron", List.of(
                new ArmorPair(Material.LEATHER_HELMET, Material.IRON_HELMET, 5),
                new ArmorPair(Material.LEATHER_CHESTPLATE, Material.IRON_CHESTPLATE, 8),
                new ArmorPair(Material.LEATHER_LEGGINGS, Material.IRON_LEGGINGS, 7),
                new ArmorPair(Material.LEATHER_BOOTS, Material.IRON_BOOTS, 4)
        ));
        ARMOR_CRAFTING.put("Golden", List.of(
                new ArmorPair(Material.LEATHER_HELMET, Material.GOLDEN_HELMET, 5),
                new ArmorPair(Material.LEATHER_CHESTPLATE, Material.GOLDEN_CHESTPLATE, 8),
                new ArmorPair(Material.LEATHER_LEGGINGS, Material.GOLDEN_LEGGINGS, 7),
                new ArmorPair(Material.LEATHER_BOOTS, Material.GOLDEN_BOOTS, 4)
        ));
        ARMOR_CRAFTING.put("Diamond", List.of(
                new ArmorPair(Material.LEATHER_HELMET, Material.DIAMOND_HELMET, 5),
                new ArmorPair(Material.LEATHER_CHESTPLATE, Material.DIAMOND_CHESTPLATE, 8),
                new ArmorPair(Material.LEATHER_LEGGINGS, Material.DIAMOND_LEGGINGS, 7),
                new ArmorPair(Material.LEATHER_BOOTS, Material.DIAMOND_BOOTS, 4)
        ));
        ARMOR_CRAFTING.put("Netherite", List.of(
                new ArmorPair(Material.LEATHER_HELMET, Material.NETHERITE_HELMET, 5),
                new ArmorPair(Material.LEATHER_CHESTPLATE, Material.NETHERITE_CHESTPLATE, 8),
                new ArmorPair(Material.LEATHER_LEGGINGS, Material.NETHERITE_LEGGINGS, 7),
                new ArmorPair(Material.LEATHER_BOOTS, Material.NETHERITE_BOOTS, 4)
        ));
    }

    private static class ArmorPair {
        Material baseArmor;
        Material resultArmor;
        int plateCount;

        ArmorPair(Material base, Material result, int plates) {
            this.baseArmor = base;
            this.resultArmor = result;
            this.plateCount = plates;
        }
    }

    private static final Map<Material, String> BASE_ITEMS = new LinkedHashMap<>();

    static {
        BASE_ITEMS.put(Material.LEATHER_HELMET, "Helmet");
        BASE_ITEMS.put(Material.LEATHER_CHESTPLATE, "Chestplate");
        BASE_ITEMS.put(Material.LEATHER_LEGGINGS, "Leggings");
        BASE_ITEMS.put(Material.LEATHER_BOOTS, "Boots");

        // Tools
        BASE_ITEMS.put(Material.STONE_PICKAXE, "Pickaxe");
        BASE_ITEMS.put(Material.STONE_AXE, "Axe");
        BASE_ITEMS.put(Material.STONE_SHOVEL, "Shovel");
        BASE_ITEMS.put(Material.STONE_SWORD, "Sword");
        BASE_ITEMS.put(Material.STONE_HOE, "Hoe");
    }

    private static final Set<NamespacedKey> BLUEPRINT_RECIPES = new HashSet<>();
    private static final Map<String, ItemStack> GENERIC_BLUEPRINT_CACHE = new HashMap<>();
    private static final Map<String, ItemStack> TIERED_BLUEPRINT_CACHE = new HashMap<>();
    private static final Map<String, ItemStack> ARMOR_PLATE_CACHE = new HashMap<>();
    private static final Map<String, ItemStack> ARMOR_PLATE_SET_CACHE = new HashMap<>();

    public static void init() {
        registerBlueprintRecipes();
        registerLeatherArmorRecipes();
        registerChainmailRecipes();
        registerArmorPlateRecipes();
        registerArmorPlateSets();
        registerSmithingTransformRecipes();
        Bukkit.getPluginManager().registerEvents(new Blueprints(), Specialization.getInstance());
    }

    private static void registerBlueprintRecipes() {
        Bukkit.getLogger().info("[Blueprints] Registering blueprint recipes...");
        int successCount = 0;
        List<String> failedExceptions = new ArrayList<>();
        for (String baseType : BASE_ITEMS.values()) {
            String genericName = "Generic " + baseType + " Blueprint";
            GENERIC_BLUEPRINT_CACHE.put(baseType, createBlueprintItem(genericName));
        }
        for (Map.Entry<Material, String> tierEntry : TIER_MATERIALS.entrySet()) {
            String tierName = tierEntry.getValue();
            for (String baseType : BASE_ITEMS.values()) {
                String key = tierName + "_" + baseType;
                TIERED_BLUEPRINT_CACHE.put(key, createBlueprintItem(tierName + " " + baseType + " Blueprint"));
            }
        }
        for (Map.Entry<Material, String> entry : BASE_ITEMS.entrySet()) {
            try {
                Material baseMaterial = entry.getKey();
                String baseType = entry.getValue();

                ItemStack genericBlueprint = GENERIC_BLUEPRINT_CACHE.get(baseType);
                NamespacedKey recipeKey = new NamespacedKey(Specialization.getInstance(),
                        "generic_" + baseType.toLowerCase() + "_blueprint");

                ShapelessRecipe genericRecipe = new ShapelessRecipe(recipeKey, genericBlueprint);
                genericRecipe.addIngredient(3, Material.PAPER);
                genericRecipe.addIngredient(Material.BLUE_DYE);
                genericRecipe.addIngredient(baseMaterial);

                if (!recipeExists(recipeKey)) {
                    Bukkit.addRecipe(genericRecipe, true);
                    BLUEPRINT_RECIPES.add(recipeKey);
                    successCount++;
                }
            } catch (Exception e) {
                failedExceptions.add(entry.getKey().getKey().value() + "_generic (" + e.getMessage() + ")");
            }
        }
        for (Map.Entry<Material, String> tierEntry : TIER_MATERIALS.entrySet()) {
            Material tierMaterial = tierEntry.getKey();
            String tierName = tierEntry.getValue();

            for (Map.Entry<Material, String> baseEntry : BASE_ITEMS.entrySet()) {
                try {
                    String baseType = baseEntry.getValue();
                    String cacheKey = tierName + "_" + baseType;
                    ItemStack tieredBlueprint = TIERED_BLUEPRINT_CACHE.get(cacheKey);
                    ItemStack genericBlueprint = GENERIC_BLUEPRINT_CACHE.get(baseType);

                    NamespacedKey recipeKey = new NamespacedKey(Specialization.getInstance(),
                            tierName.toLowerCase() + "_" + baseType.toLowerCase() + "_blueprint");

                    ShapelessRecipe tieredRecipe = new ShapelessRecipe(recipeKey, tieredBlueprint);
                    tieredRecipe.addIngredient(new RecipeChoice.ExactChoice(genericBlueprint));
                    tieredRecipe.addIngredient(tierMaterial);

                    if (!recipeExists(recipeKey)) {
                        Bukkit.addRecipe(tieredRecipe, true);
                        BLUEPRINT_RECIPES.add(recipeKey);
                        successCount++;
                    }
                } catch (Exception e) {
                    failedExceptions.add(tierMaterial.getKey().value() + "_" + baseEntry.getKey().getKey().value()
                            + "_blueprint (" + e.getMessage() + ")");
                }
            }
        }

        Bukkit.getLogger().info("[Blueprints] Blueprint recipes: " + successCount + " registered.");
        if (!failedExceptions.isEmpty()) {
            Bukkit.getLogger().warning("[Blueprints] Failed recipes (" + failedExceptions.size() + "):");
            failedExceptions.forEach(f -> Bukkit.getLogger().warning(" - " + f));
        }
    }

    private static void registerLeatherArmorRecipes() {
        Bukkit.getLogger().info("[Blueprints] Registering leather armor recipes...");
        int successCount = 0;

        try {
            ItemStack paddedLeather = new ItemStack(Material.LEATHER);
            ItemMeta meta = paddedLeather.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text("Padded Leather").color(NamedTextColor.LIGHT_PURPLE));
                CustomModelDataComponent cmdComponent = meta.getCustomModelDataComponent();
                cmdComponent.setStrings(List.of("padded_leather"));
                meta.setCustomModelDataComponent(cmdComponent);
                paddedLeather.setItemMeta(meta);
            }

            NamespacedKey paddedKey = new NamespacedKey(Specialization.getInstance(), "padded_leather");
            ShapedRecipe paddedRecipe = new ShapedRecipe(paddedKey, paddedLeather);
            paddedRecipe.shape("###", "WWW", "   ");
            paddedRecipe.setIngredient('#', Material.LEATHER);
            paddedRecipe.setIngredient('W', Material.WHITE_WOOL);

            if (!recipeExists(paddedKey)) {
                Bukkit.addRecipe(paddedRecipe, true);
                successCount++;
            }
            ItemStack paddedLeatherForRecipes = new ItemStack(Material.LEATHER);
            ItemMeta paddedMeta = paddedLeatherForRecipes.getItemMeta();
            if (paddedMeta != null) {
                paddedMeta.displayName(Component.text("Padded Leather").color(NamedTextColor.LIGHT_PURPLE));
                CustomModelDataComponent paddedCmd = paddedMeta.getCustomModelDataComponent();
                paddedCmd.setStrings(List.of("padded_leather"));
                paddedMeta.setCustomModelDataComponent(paddedCmd);
                paddedLeatherForRecipes.setItemMeta(paddedMeta);
            }

            NamespacedKey helmetKey = new NamespacedKey(Specialization.getInstance(), "padded_leather_helmet");
            ShapedRecipe helmetRecipe = new ShapedRecipe(helmetKey, new ItemStack(Material.LEATHER_HELMET));
            helmetRecipe.shape("###", "# #", "   ");
            helmetRecipe.setIngredient('#', new RecipeChoice.ExactChoice(paddedLeatherForRecipes));

            if (!recipeExists(helmetKey)) {
                Bukkit.addRecipe(helmetRecipe, true);
                successCount++;
            }

            NamespacedKey chestKey = new NamespacedKey(Specialization.getInstance(), "padded_leather_chestplate");
            ShapedRecipe chestRecipe = new ShapedRecipe(chestKey, new ItemStack(Material.LEATHER_CHESTPLATE));
            chestRecipe.shape("# #", "###", "###");
            chestRecipe.setIngredient('#', new RecipeChoice.ExactChoice(paddedLeatherForRecipes));

            if (!recipeExists(chestKey)) {
                Bukkit.addRecipe(chestRecipe, true);
                successCount++;
            }
            NamespacedKey leggingsKey = new NamespacedKey(Specialization.getInstance(), "padded_leather_leggings");
            ShapedRecipe leggingsRecipe = new ShapedRecipe(leggingsKey, new ItemStack(Material.LEATHER_LEGGINGS));
            leggingsRecipe.shape("###", "# #", "# #");
            leggingsRecipe.setIngredient('#', new RecipeChoice.ExactChoice(paddedLeatherForRecipes));

            if (!recipeExists(leggingsKey)) {
                Bukkit.addRecipe(leggingsRecipe, true);
                successCount++;
            }
            NamespacedKey bootsKey = new NamespacedKey(Specialization.getInstance(), "padded_leather_boots");
            ShapedRecipe bootsRecipe = new ShapedRecipe(bootsKey, new ItemStack(Material.LEATHER_BOOTS));
            bootsRecipe.shape("# #", "# #", "   ");
            bootsRecipe.setIngredient('#', new RecipeChoice.ExactChoice(paddedLeatherForRecipes));

            if (!recipeExists(bootsKey)) {
                Bukkit.addRecipe(bootsRecipe, true);
                successCount++;
            }

        } catch (Exception e) {
            Bukkit.getLogger().warning("[Blueprints] Failed to register leather armor recipes: " + e.getMessage());
        }

        Bukkit.getLogger().info("[Blueprints] Leather armor recipes: " + successCount + " registered.");
    }


    private static void registerChainmailRecipes() {
        Bukkit.getLogger().info("[Blueprints] Registering chainmail recipes...");
        int successCount = 0;

        try {
            ItemStack chainmailPlate = new ItemStack(Material.IRON_INGOT);
            ItemMeta chainmailMeta = chainmailPlate.getItemMeta();
            if (chainmailMeta != null) {
                chainmailMeta.displayName(Component.text("Chainmail Armor Plate").color(NamedTextColor.LIGHT_PURPLE));
                CustomModelDataComponent chainmailCmd = chainmailMeta.getCustomModelDataComponent();
                chainmailCmd.setStrings(List.of("armor_plate_chainmail"));
                chainmailMeta.setCustomModelDataComponent(chainmailCmd);
                chainmailPlate.setItemMeta(chainmailMeta);
            }

            NamespacedKey chainmailPlateKey = new NamespacedKey(Specialization.getInstance(), "chainmail_armor_plate");
            ShapedRecipe chainmailPlateRecipe = new ShapedRecipe(chainmailPlateKey, chainmailPlate);
            chainmailPlateRecipe.shape("###", "###", "   ");
            chainmailPlateRecipe.setIngredient('#', Material.CHAIN);

            if (!recipeExists(chainmailPlateKey)) {
                Bukkit.addRecipe(chainmailPlateRecipe, true);
                successCount++;
            }
            ItemStack chainmailPlateForRecipes = new ItemStack(Material.IRON_INGOT);
            ItemMeta chainmailMetaForRecipes = chainmailPlateForRecipes.getItemMeta();
            if (chainmailMetaForRecipes != null) {
                chainmailMetaForRecipes.displayName(Component.text("Chainmail Armor Plate").color(NamedTextColor.LIGHT_PURPLE));
                CustomModelDataComponent chainmailCmdForRecipes = chainmailMetaForRecipes.getCustomModelDataComponent();
                chainmailCmdForRecipes.setStrings(List.of("armor_plate_chainmail"));
                chainmailMetaForRecipes.setCustomModelDataComponent(chainmailCmdForRecipes);
                chainmailPlateForRecipes.setItemMeta(chainmailMetaForRecipes);
            }

            NamespacedKey chainmailHelmetKey = new NamespacedKey(Specialization.getInstance(), "chainmail_helmet");
            ShapedRecipe chainmailHelmetRecipe = new ShapedRecipe(chainmailHelmetKey, new ItemStack(Material.CHAINMAIL_HELMET));
            chainmailHelmetRecipe.shape("###", "# #", "   ");
            chainmailHelmetRecipe.setIngredient('#', new RecipeChoice.ExactChoice(chainmailPlateForRecipes));

            if (!recipeExists(chainmailHelmetKey)) {
                Bukkit.addRecipe(chainmailHelmetRecipe, true);
                successCount++;
            }
            NamespacedKey chainmailChestKey = new NamespacedKey(Specialization.getInstance(), "chainmail_chestplate");
            ShapedRecipe chainmailChestRecipe = new ShapedRecipe(chainmailChestKey, new ItemStack(Material.CHAINMAIL_CHESTPLATE));
            chainmailChestRecipe.shape("# #", "###", "###");
            chainmailChestRecipe.setIngredient('#', new RecipeChoice.ExactChoice(chainmailPlateForRecipes));

            if (!recipeExists(chainmailChestKey)) {
                Bukkit.addRecipe(chainmailChestRecipe, true);
                successCount++;
            }
            NamespacedKey chainmailLeggingsKey = new NamespacedKey(Specialization.getInstance(), "chainmail_leggings");
            ShapedRecipe chainmailLeggingsRecipe = new ShapedRecipe(chainmailLeggingsKey, new ItemStack(Material.CHAINMAIL_LEGGINGS));
            chainmailLeggingsRecipe.shape("###", "# #", "# #");
            chainmailLeggingsRecipe.setIngredient('#', new RecipeChoice.ExactChoice(chainmailPlateForRecipes));

            if (!recipeExists(chainmailLeggingsKey)) {
                Bukkit.addRecipe(chainmailLeggingsRecipe, true);
                successCount++;
            }
            NamespacedKey chainmailBootsKey = new NamespacedKey(Specialization.getInstance(), "chainmail_boots");
            ShapedRecipe chainmailBootsRecipe = new ShapedRecipe(chainmailBootsKey, new ItemStack(Material.CHAINMAIL_BOOTS));
            chainmailBootsRecipe.shape("# #", "# #", "   ");
            chainmailBootsRecipe.setIngredient('#', new RecipeChoice.ExactChoice(chainmailPlateForRecipes));

            if (!recipeExists(chainmailBootsKey)) {
                Bukkit.addRecipe(chainmailBootsRecipe, true);
                successCount++;
            }

        } catch (Exception e) {
            Bukkit.getLogger().warning("[Blueprints] Failed to register chainmail recipes: " + e.getMessage());
        }

        Bukkit.getLogger().info("[Blueprints] Chainmail recipes: " + successCount + " registered.");
    }

    private static void registerArmorPlateRecipes() {
        Bukkit.getLogger().info("[Blueprints] Registering armor plate recipes...");
        int successCount = 0;

        try {
            ItemStack ironPlate = new ItemStack(Material.IRON_INGOT);
            ItemMeta ironMeta = ironPlate.getItemMeta();
            if (ironMeta != null) {
                ironMeta.displayName(Component.text("Iron Armor Plate").color(NamedTextColor.LIGHT_PURPLE));
                CustomModelDataComponent ironCmd = ironMeta.getCustomModelDataComponent();
                ironCmd.setStrings(List.of("armor_plate_iron"));
                ironMeta.setCustomModelDataComponent(ironCmd);
                ironPlate.setItemMeta(ironMeta);
            }

            NamespacedKey ironPlateKey = new NamespacedKey(Specialization.getInstance(), "iron_armor_plate");
            ShapedRecipe ironPlateRecipe = new ShapedRecipe(ironPlateKey, ironPlate);
            ironPlateRecipe.shape("###", "###", "   ");
            ironPlateRecipe.setIngredient('#', Material.IRON_INGOT);

            if (!recipeExists(ironPlateKey)) {
                Bukkit.addRecipe(ironPlateRecipe, true);
                successCount++;
            }

            ItemStack goldPlate = new ItemStack(Material.GOLD_INGOT);
            ItemMeta goldMeta = goldPlate.getItemMeta();
            if (goldMeta != null) {
                goldMeta.displayName(Component.text("Gold Armor Plate").color(NamedTextColor.LIGHT_PURPLE));
                CustomModelDataComponent goldCmd = goldMeta.getCustomModelDataComponent();
                goldCmd.setStrings(List.of("armor_plate_gold"));
                goldMeta.setCustomModelDataComponent(goldCmd);
                goldPlate.setItemMeta(goldMeta);
            }

            NamespacedKey goldPlateKey = new NamespacedKey(Specialization.getInstance(), "gold_armor_plate");
            ShapedRecipe goldPlateRecipe = new ShapedRecipe(goldPlateKey, goldPlate);
            goldPlateRecipe.shape("###", "###", "   ");
            goldPlateRecipe.setIngredient('#', Material.GOLD_INGOT);

            if (!recipeExists(goldPlateKey)) {
                Bukkit.addRecipe(goldPlateRecipe, true);
                successCount++;
            }

            ItemStack diamondPlate = new ItemStack(Material.DIAMOND);
            ItemMeta diamondMeta = diamondPlate.getItemMeta();
            if (diamondMeta != null) {
                diamondMeta.displayName(Component.text("Diamond Armor Plate").color(NamedTextColor.LIGHT_PURPLE));
                CustomModelDataComponent diamondCmd = diamondMeta.getCustomModelDataComponent();
                diamondCmd.setStrings(List.of("armor_plate_diamond"));
                diamondMeta.setCustomModelDataComponent(diamondCmd);
                diamondPlate.setItemMeta(diamondMeta);
            }

            ItemStack ironPlateForDiamond = new ItemStack(Material.IRON_INGOT);
            ItemMeta ironMetaForDiamond = ironPlateForDiamond.getItemMeta();
            if (ironMetaForDiamond != null) {
                ironMetaForDiamond.displayName(Component.text("Iron Armor Plate").color(NamedTextColor.LIGHT_PURPLE));
                CustomModelDataComponent ironCmdForDiamond = ironMetaForDiamond.getCustomModelDataComponent();
                ironCmdForDiamond.setStrings(List.of("armor_plate_iron"));
                ironMetaForDiamond.setCustomModelDataComponent(ironCmdForDiamond);
                ironPlateForDiamond.setItemMeta(ironMetaForDiamond);
            }

            NamespacedKey diamondPlateKey = new NamespacedKey(Specialization.getInstance(), "diamond_armor_plate");
            ShapelessRecipe diamondPlateRecipe = new ShapelessRecipe(diamondPlateKey, diamondPlate);
            diamondPlateRecipe.addIngredient(new RecipeChoice.ExactChoice(ironPlateForDiamond));
            diamondPlateRecipe.addIngredient(2, Material.DIAMOND);

            if (!recipeExists(diamondPlateKey)) {
                Bukkit.addRecipe(diamondPlateRecipe, true);
                successCount++;
            }

            ItemStack netheritePlate = new ItemStack(Material.NETHERITE_INGOT);
            ItemMeta netheriteMeta = netheritePlate.getItemMeta();
            if (netheriteMeta != null) {
                netheriteMeta.displayName(Component.text("Netherite Armor Plate").color(NamedTextColor.LIGHT_PURPLE));
                CustomModelDataComponent netheriteCmd = netheriteMeta.getCustomModelDataComponent();
                netheriteCmd.setStrings(List.of("armor_plate_netherite"));
                netheriteMeta.setCustomModelDataComponent(netheriteCmd);
                netheritePlate.setItemMeta(netheriteMeta);
            }

            ItemStack diamondPlateItem = new ItemStack(Material.DIAMOND);
            ItemMeta diamondPlateMeta = diamondPlateItem.getItemMeta();
            if (diamondPlateMeta != null) {
                diamondPlateMeta.displayName(Component.text("Diamond Armor Plate").color(NamedTextColor.LIGHT_PURPLE));
                CustomModelDataComponent diamondPlateCmd = diamondPlateMeta.getCustomModelDataComponent();
                diamondPlateCmd.setStrings(List.of("armor_plate_diamond"));
                diamondPlateMeta.setCustomModelDataComponent(diamondPlateCmd);
                diamondPlateItem.setItemMeta(diamondPlateMeta);
            }

            NamespacedKey netheritePlateKey = new NamespacedKey(Specialization.getInstance(), "netherite_armor_plate");
            ShapelessRecipe netheritePlateRecipe = new ShapelessRecipe(netheritePlateKey, netheritePlate);
            netheritePlateRecipe.addIngredient(new RecipeChoice.ExactChoice(diamondPlateItem));
            netheritePlateRecipe.addIngredient(2, Material.NETHERITE_INGOT);

            if (!recipeExists(netheritePlateKey)) {
                Bukkit.addRecipe(netheritePlateRecipe, true);
                successCount++;
            }

        } catch (Exception e) {
            Bukkit.getLogger().warning("[Blueprints] Failed to register armor plate recipes: " + e.getMessage());
        }

        Bukkit.getLogger().info("[Blueprints] Armor plate recipes: " + successCount + " registered.");
    }
    private static void registerArmorPlateSets() {
        Bukkit.getLogger().info("[Blueprints] Registering armor plate set recipes...");
        int successCount = 0;
        for (Map.Entry<String, Material> materialEntry : ARMOR_PLATE_MATERIALS.entrySet()) {
            String tier = materialEntry.getKey();
            Material plateType = materialEntry.getValue();
            if (!ARMOR_PLATE_CACHE.containsKey(tier)) {
                ARMOR_PLATE_CACHE.put(tier, createArmorPlate(tier, plateType));
            }
        }
        for (Map.Entry<String, Material> materialEntry : ARMOR_PLATE_MATERIALS.entrySet()) {
            String tier = materialEntry.getKey();
            Material plateType = materialEntry.getValue();

            for (String armorType : List.of("Helmet", "Chestplate", "Leggings", "Boots")) {
                String key = tier + "_" + armorType;
                ARMOR_PLATE_SET_CACHE.put(key, createArmorPlateSet(tier, armorType, plateType));
            }
        }
        for (Map.Entry<String, Material> materialEntry : ARMOR_PLATE_MATERIALS.entrySet()) {
            String tier = materialEntry.getKey();

            try {
                ItemStack armorPlate = ARMOR_PLATE_CACHE.get(tier);
                ItemStack helmetSet = ARMOR_PLATE_SET_CACHE.get(tier + "_Helmet");
                NamespacedKey helmetKey = new NamespacedKey(Specialization.getInstance(),
                        tier.toLowerCase() + "_helmet_armor_plateset");
                ShapedRecipe helmetRecipe = new ShapedRecipe(helmetKey, helmetSet);
                helmetRecipe.shape("###", "# #", "   ");
                helmetRecipe.setIngredient('#', new RecipeChoice.ExactChoice(armorPlate));

                if (!recipeExists(helmetKey)) {
                    Bukkit.addRecipe(helmetRecipe, true);
                    successCount++;
                }
                ItemStack chestSet = ARMOR_PLATE_SET_CACHE.get(tier + "_Chestplate");
                NamespacedKey chestKey = new NamespacedKey(Specialization.getInstance(),
                        tier.toLowerCase() + "_chestplate_armor_plateset");
                ShapedRecipe chestRecipe = new ShapedRecipe(chestKey, chestSet);
                chestRecipe.shape("# #", "###", "###");
                chestRecipe.setIngredient('#', new RecipeChoice.ExactChoice(armorPlate));

                if (!recipeExists(chestKey)) {
                    Bukkit.addRecipe(chestRecipe, true);
                    successCount++;
                }

                ItemStack leggingsSet = ARMOR_PLATE_SET_CACHE.get(tier + "_Leggings");
                NamespacedKey leggingsKey = new NamespacedKey(Specialization.getInstance(),
                        tier.toLowerCase() + "_leggings_armor_plateset");
                ShapedRecipe leggingsRecipe = new ShapedRecipe(leggingsKey, leggingsSet);
                leggingsRecipe.shape("###", "# #", "# #");
                leggingsRecipe.setIngredient('#', new RecipeChoice.ExactChoice(armorPlate));

                if (!recipeExists(leggingsKey)) {
                    Bukkit.addRecipe(leggingsRecipe, true);
                    successCount++;
                }

                // Boots - uses SAME cached armorPlate
                ItemStack bootsSet = ARMOR_PLATE_SET_CACHE.get(tier + "_Boots");
                NamespacedKey bootsKey = new NamespacedKey(Specialization.getInstance(),
                        tier.toLowerCase() + "_boots_armor_plateset");
                ShapedRecipe bootsRecipe = new ShapedRecipe(bootsKey, bootsSet);
                bootsRecipe.shape("# #", "# #", "   ");
                bootsRecipe.setIngredient('#', new RecipeChoice.ExactChoice(armorPlate));

                if (!recipeExists(bootsKey)) {
                    Bukkit.addRecipe(bootsRecipe, true);
                    successCount++;
                }

            } catch (Exception e) {
                Bukkit.getLogger().warning("[Blueprints] Failed to register armor plate sets for " + tier + ": " + e.getMessage());
            }
        }

        Bukkit.getLogger().info("[Blueprints] Armor plate set recipes: " + successCount + " registered.");
    }

    private static ItemStack createArmorPlate(String tier, Material baseType) {
        ItemStack plate = new ItemStack(baseType);
        ItemMeta meta = plate.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(tier + " Armor Plate").color(NamedTextColor.LIGHT_PURPLE));
            CustomModelDataComponent cmdComponent = meta.getCustomModelDataComponent();
            cmdComponent.setStrings(List.of("armor_plate_" + tier.toLowerCase()));
            meta.setCustomModelDataComponent(cmdComponent);
            plate.setItemMeta(meta);
        }
        return plate;
    }

    private static ItemStack createArmorPlateSet(String tier, String armorType, Material plateType) {
        ItemStack plateSet = new ItemStack(plateType);
        ItemMeta meta = plateSet.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(tier + " " + armorType + " Armor Plate Set").color(NamedTextColor.LIGHT_PURPLE));
            CustomModelDataComponent cmdComponent = meta.getCustomModelDataComponent();
            cmdComponent.setStrings(List.of(tier.toLowerCase() + "_armor_plateset"));
            meta.setCustomModelDataComponent(cmdComponent);
            plateSet.setItemMeta(meta);
        }
        return plateSet;
    }

    private static void registerSmithingTransformRecipes() {
        Bukkit.getLogger().info("[Blueprints] Registering smithing table armor recipes...");
        int successCount = 0;
        List<String> failedExceptions = new ArrayList<>();

        for (Map.Entry<String, List<ArmorPair>> tierEntry : ARMOR_CRAFTING.entrySet()) {
            String tier = tierEntry.getKey();
            List<ArmorPair> armorPairs = tierEntry.getValue();

            for (ArmorPair pair : armorPairs) {
                try {
                    String armorType = pair.resultArmor.name().replaceAll("^[A-Z]+_", "").replace("_", " ");
                    armorType = armorType.substring(0, 1).toUpperCase() + armorType.substring(1).toLowerCase();

                    ItemStack result = new ItemStack(pair.resultArmor);

                    String blueprintKey = tier + "_" + armorType;
                    ItemStack tieredBlueprint = TIERED_BLUEPRINT_CACHE.get(blueprintKey);
                    String plateSetKey = tier + "_" + armorType;
                    ItemStack plateSet = ARMOR_PLATE_SET_CACHE.get(plateSetKey);

                    String armorTypeLower = armorType.replace(" ", "").toLowerCase();
                    NamespacedKey recipeKey = new NamespacedKey(Specialization.getInstance(),
                            "smithing_" + tier.toLowerCase() + "_" + armorTypeLower);

                    SmithingTransformRecipe smithingRecipe = new SmithingTransformRecipe(
                            recipeKey,
                            result,
                            new RecipeChoice.ExactChoice(tieredBlueprint),
                            new RecipeChoice.MaterialChoice(pair.baseArmor),
                            new RecipeChoice.ExactChoice(plateSet),
                            false
                    );

                    if (!recipeExists(recipeKey)) {
                        Bukkit.addRecipe(smithingRecipe, true);
                        BLUEPRINT_RECIPES.add(recipeKey);
                        successCount++;
                    }
                } catch (Exception e) {
                    failedExceptions.add(tier + "_" + pair.resultArmor.name() + " (" + e.getMessage() + ")");
                }
            }
        }

        Bukkit.getLogger().info("[Blueprints] Smithing table armor recipes: " + successCount + " registered.");
        if (!failedExceptions.isEmpty()) {
            Bukkit.getLogger().warning("[Blueprints] Failed smithing recipes (" + failedExceptions.size() + "):");
            failedExceptions.forEach(f -> Bukkit.getLogger().warning(" - " + f));
        }
    }

    private static ItemStack createBlueprintItem(String name) {
        ItemStack blueprint = new ItemStack(Material.PAPER);
        ItemMeta meta = blueprint.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text(name).color(NamedTextColor.LIGHT_PURPLE));
            meta.lore(List.of(
                    Component.empty(),
                    Component.text("Use in the Smithing Table").color(NamedTextColor.GRAY),
                    Component.text("to upgrade armor.").color(NamedTextColor.GRAY)
            ));

            CustomModelDataComponent cmdComponent = meta.getCustomModelDataComponent();
            cmdComponent.setStrings(List.of("blueprint"));
            meta.setCustomModelDataComponent(cmdComponent);

            blueprint.setItemMeta(meta);
        }

        return blueprint;
    }

    private static boolean recipeExists(NamespacedKey key) {
        Iterator<Recipe> iterator = Bukkit.recipeIterator();
        while (iterator.hasNext()) {
            Recipe recipe = iterator.next();
            if (recipe instanceof Keyed keyed && keyed.getKey().equals(key)) {
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        Recipe recipe = event.getRecipe();

        if (!(recipe instanceof Keyed keyed) || !BLUEPRINT_RECIPES.contains(keyed.getKey())) {
            return;
        }

        if (event.getInventory() instanceof SmithingInventory smithingInv) {
            ItemStack template = smithingInv.getItem(0);
            if (template != null && isBlueprintItem(template)) {
                ItemStack toReturn = template.clone();
                toReturn.setAmount(1);

                if (!event.getWhoClicked().getInventory().addItem(toReturn).isEmpty()) {
                    event.getWhoClicked().getWorld().dropItem(
                            event.getWhoClicked().getLocation(),
                            toReturn
                    );
                }
            }
            return;
        }

        CraftingInventory inventory = event.getInventory();
        ItemStack[] matrix = inventory.getMatrix();

        for (int i = 0; i < matrix.length; i++) {
            ItemStack item = matrix[i];
            if (item != null && isBlueprintItem(item)) {
                ItemStack toReturn = item.clone();
                toReturn.setAmount(1);

                if (!event.getWhoClicked().getInventory().addItem(toReturn).isEmpty()) {
                    event.getWhoClicked().getWorld().dropItem(
                            event.getWhoClicked().getLocation(),
                            toReturn
                    );
                }
                matrix[i] = null;
                break;
            }
        }
    }

    private static boolean isBlueprintItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;

        Component displayName = meta.displayName();
        if (displayName == null) return false;

        String plainText = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(displayName);
        return plainText.contains("Blueprint");
    }

    public static void listRecipes() {
        Iterator<Recipe> iterator = Bukkit.recipeIterator();
        int count = 0;
        while (iterator.hasNext()) {
            Recipe recipe = iterator.next();
            if (recipe instanceof Keyed keyed) {
                String key = keyed.getKey().toString();
                if (key.contains("specialization")) {
                    Bukkit.getLogger().info("[Blueprints] Registered: " + key);
                    count++;
                }
            }
        }
        Bukkit.getLogger().info("[Blueprints] Total custom recipes: " + count);
    }
}