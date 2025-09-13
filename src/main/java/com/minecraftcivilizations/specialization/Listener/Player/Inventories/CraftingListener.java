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

        CustomPlayer customPlayer = (CustomPlayer) MinecraftCivilizationsCore.getInstance()
                .getCustomPlayerManager().getCustomPlayer(player.getUniqueId());

        int craftedAmount = (int) (getCraftedAmount(event) / (Math.random() * 3 + 1));

        int reduction = (int) ((5-customPlayer.getSkillLevel(SkillType.BLACKSMITH))/1.5) * craftedAmount;
        reduction -= (int) (Math.random() * 4);

        int foodLevel = player.getFoodLevel();

        if(foodLevel < reduction){
            event.setCancelled(true);
            return;
        }

        ItemStack crafted = event.getCurrentItem();


        Pair<SkillType, Double> pair = SpecializationConfig.getXpGainFromCraftingConfig()
                .get(crafted.getType(), new TypeToken<>() {});

        if (pair != null && pair.firstValue() != null && pair.secondValue() != null) {
            double xpToGive = pair.secondValue() * craftedAmount;

            int finalReduction = reduction;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    customPlayer.addSkillXp(pair.firstValue(), xpToGive);
                    LOGGER.fine("Gave " + xpToGive + " XP to " + player.getName() +
                            " for crafting " + craftedAmount + "x " + crafted.getType());
                    if(finalReduction > 0) {
                        player.setFoodLevel(player.getFoodLevel() - finalReduction);
                    }
                }
            }, 1L);
        }

    }

    /**
     * Determines if the crafting action will actually consume ingredients
     * and produce items in the player's inventory.
     */
    private boolean isCraftingActionValid(CraftItemEvent event) {
        InventoryAction action = event.getAction();

        return switch (action) {
            case PICKUP_ALL, PICKUP_SOME, PICKUP_HALF, PICKUP_ONE, MOVE_TO_OTHER_INVENTORY, PLACE_ALL, PLACE_SOME,
                 PLACE_ONE, SWAP_WITH_CURSOR, HOTBAR_SWAP, DROP_ALL_CURSOR, DROP_ALL_SLOT, DROP_ONE_CURSOR, DROP_ONE_SLOT -> true;
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
            if (!player.hasDiscoveredRecipe(recipeKey)) {
                player.discoverRecipe(recipeKey);
                LOGGER.fine("Discovered recipe " + recipeKey + " for player " + player.getName() + " on-demand");
            }
        }
    }

    public static boolean shouldBlockRecipe(Player player, NamespacedKey recipeKey) {
        CustomPlayer customPlayer = (CustomPlayer) MinecraftCivilizationsCore.getInstance()
                .getCustomPlayerManager().getCustomPlayer(player.getUniqueId());
        if(customPlayer.getAdditionUnlockedRecipes() != null && customPlayer.getAdditionUnlockedRecipes().contains(recipeKey)) return true;

        // Check if recipe is in any skill-specific unlocked recipes config
        for (SkillType skillType : SkillType.values()) {
            for (SkillLevel skillLevel : SkillLevel.values()) {
                String configKey = skillType + "_" + skillLevel;
                Set<NamespacedKey> skillRecipes = SpecializationConfig.getUnlockedRecipesConfig()
                        .get(configKey, new TypeToken<>() {
                        });

                if (skillRecipes != null && skillRecipes.contains(recipeKey)) {
                    return customPlayer.getSkillLevel(skillType) < skillLevel.ordinal();
                }
            }
        }

        // Recipe is not in any skill config - allow it
        return false;
    }
}