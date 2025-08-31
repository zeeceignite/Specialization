package com.minecraftcivilizations.specialization.Listener.Player.Inventories;

import com.google.gson.reflect.TypeToken;
import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillLevel;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import minecraftcivilizations.com.minecraftCivilizationsCore.MinecraftCivilizationsCore;
import minecraftcivilizations.com.minecraftCivilizationsCore.Options.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.plugin.Plugin;

import java.util.Set;
import java.util.logging.Logger;

public class CraftingListener implements Listener {
    private static final Logger LOGGER = Logger.getLogger(CraftingListener.class.getName());
    private final Plugin plugin;

    public CraftingListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || event.getCurrentItem() == null) return;

        if (!isCraftingActionValid(event)) {
            return;
        }

        ItemStack crafted = event.getCurrentItem();

        CustomPlayer customPlayer = (CustomPlayer) MinecraftCivilizationsCore.getInstance()
                .getCustomPlayerManager().getCustomPlayer(player.getUniqueId());

        if(player.getFoodLevel() == 0 && customPlayer.getSkillLevel(SkillType.BLACKSMITH) < 5) return;


        Pair<SkillType, Double> pair = SpecializationConfig.getXpGainFromCraftingConfig()
                .get(crafted.getType(), new TypeToken<>() {});

        if (pair != null && pair.firstValue() != null && pair.secondValue() != null) {
            int craftedAmount = getCraftedAmount(event);
            double xpToGive = pair.secondValue() * craftedAmount;

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    customPlayer.addSkillXp(pair.firstValue(), xpToGive);
                    LOGGER.fine("Gave " + xpToGive + " XP to " + player.getName() +
                            " for crafting " + craftedAmount + "x " + crafted.getType());
                }
            }, 1L);
        }
        int reduction = 5 - Math.min(3,5-customPlayer.getSkillLevel(SkillType.BLACKSMITH));
        player.setFoodLevel(player.getFoodLevel() - reduction);
    }

    /**
     * Determines if the crafting action will actually consume ingredients
     * and produce items in the player's inventory.
     */
    private boolean isCraftingActionValid(CraftItemEvent event) {
        InventoryAction action = event.getAction();

        return switch (action) {
            case PICKUP_ALL, PICKUP_SOME, PICKUP_HALF, PICKUP_ONE, MOVE_TO_OTHER_INVENTORY, PLACE_ALL, PLACE_SOME,
                 PLACE_ONE, SWAP_WITH_CURSOR -> true;
            default -> false;
        };
    }

    /**
     * Calculates how many items are actually being crafted based on the event action
     */
    private int getCraftedAmount(CraftItemEvent event) {
        ItemStack result = event.getCurrentItem();
        if (result == null) return 0;

        InventoryAction action = event.getAction();

        switch (action) {
            case PICKUP_HALF:
                return Math.max(1, result.getAmount() / 2);
            case PICKUP_ONE:
                return 1;
            case PICKUP_SOME:
                ItemStack cursor = event.getCursor();
                if (cursor.isSimilar(result)) {
                    int maxStack = result.getMaxStackSize();
                    int canTake = maxStack - cursor.getAmount();
                    return Math.min(canTake, result.getAmount());
                }
                return result.getAmount();
            case MOVE_TO_OTHER_INVENTORY:
                return calculateBulkCraftAmount(event);
            case PICKUP_ALL:
            default:
                return result.getAmount();
        }
    }

    /**
     * Calculates the actual number of crafting operations for bulk crafting (Shift+Click)
     */
    private int calculateBulkCraftAmount(CraftItemEvent event) {
        ItemStack result = event.getCurrentItem();
        if (result == null) return 0;

        // Get the recipe and check ingredient availability
        Recipe recipe = event.getRecipe();
        if (recipe == null) return 0;

        // For bulk crafting, we need to determine how many times the recipe can be executed
        // based on available ingredients in the crafting matrix
        org.bukkit.inventory.CraftingInventory craftingInventory = event.getInventory();
        ItemStack[] matrix = craftingInventory.getMatrix();

        int maxCrafts = Integer.MAX_VALUE;

        // Check each ingredient slot to find the limiting factor
        for (ItemStack ingredient : matrix) {
            if (ingredient != null && ingredient.getAmount() > 0) {
                // Each crafting operation consumes 1 of this ingredient
                maxCrafts = Math.min(maxCrafts, ingredient.getAmount());
            }
        }

        // If no ingredients found or unlimited, default to result amount divided by recipe yield
        if (maxCrafts == Integer.MAX_VALUE) {
            return result.getAmount();
        }

        // The actual number of items crafted is maxCrafts * result.getAmount() per craft
        // But we want the number of crafting operations, so return maxCrafts
        return maxCrafts;
    }

    /**
     * Check crafting permissions when a recipe is prepared in the crafting matrix.
     * This prevents unauthorized recipes from being shown to players and handles
     * on-demand recipe discovery for allowed recipes.
     */
    @EventHandler(ignoreCancelled = true)
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        if (event.getInventory().getViewers().isEmpty()) return;

        Player player = null;

        for (var viewer : event.getInventory().getViewers()) {
            if (viewer instanceof Player) {
                player = (Player) viewer;
                break;
            }
        }

        if (player == null) return;

        Recipe recipe = event.getRecipe();
        if (recipe == null) return;

        if (!(recipe instanceof Keyed)) {
            LOGGER.warning("Recipe is not Keyed, cannot get NamespacedKey: " + recipe.getClass().getSimpleName());
            return;
        }

        NamespacedKey recipeKey = ((Keyed) recipe).getKey();

        if (shouldBlockRecipe(player, recipeKey)) {
            LOGGER.info("Blocking recipe " + recipeKey + " for player " + player.getName() + " due to insufficient skill level");
            event.getInventory().setResult(null);
            player.undiscoverRecipe(recipeKey);
        } else {
            // Auto-discover recipe if player doesn't have it yet
            if (!player.hasDiscoveredRecipe(recipeKey)) {
                player.discoverRecipe(recipeKey);
                LOGGER.fine("Auto-discovered recipe " + recipeKey + " for player " + player.getName());
            }
        }
    }

    public static boolean shouldBlockRecipe(Player player, NamespacedKey recipeKey) {
        CustomPlayer customPlayer = (CustomPlayer) MinecraftCivilizationsCore.getInstance()
                .getCustomPlayerManager().getCustomPlayer(player.getUniqueId());

        // Priority 1: additionalUnlockedRecipes (player-specific) always allowed
        if (customPlayer.getAdditionUnlockedRecipes() != null &&
            customPlayer.getAdditionUnlockedRecipes().contains(recipeKey)) {
            return false;
        }

        // Priority 2: Check if recipe is in skill-specific unlocked recipes config (skill-restricted)
        for (SkillType skillType : SkillType.values()) {
            for (SkillLevel skillLevel : SkillLevel.values()) {
                String configKey = skillType + "_" + skillLevel;
                Set<NamespacedKey> skillRecipes = SpecializationConfig.getUnlockedRecipesConfig()
                        .get(configKey, new TypeToken<>() {
                        });

                if (skillRecipes != null && skillRecipes.contains(recipeKey)) {
                    // Recipe is skill-locked, check if player meets requirements
                    return customPlayer.getSkillLevel(skillType) < skillLevel.ordinal();
                }
            }
        }

        // Priority 3: Check if recipe is in allRecipeBank (default allowed recipes)
        Set<NamespacedKey> allRecipes = SpecializationConfig.getAllRecipeBank()
                .get("ALL_RECIPES", new TypeToken<>() {
                });
        
        if (allRecipes != null && allRecipes.contains(recipeKey)) {
            // Recipe is in allRecipeBank and NOT skill-locked, so allow it
            return false;
        }

        // Priority 4: Unknown recipes blocked by default
        return false;
    }
}