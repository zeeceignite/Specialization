package com.minecraftcivilizations.specialization.Listener;

import com.google.gson.reflect.TypeToken;
import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillLevel;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import minecraftcivilizations.com.minecraftCivilizationsCore.MinecraftCivilizationsCore;
import minecraftcivilizations.com.minecraftCivilizationsCore.Options.Pair;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.Set;
import java.util.logging.Logger;

public class CraftingListener implements Listener {
    private static final Logger LOGGER = Logger.getLogger(CraftingListener.class.getName());

    @EventHandler(ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || event.getCurrentItem() == null) return;
        ItemStack crafted = event.getCurrentItem(); // The item being crafted

        CustomPlayer customPlayer = (CustomPlayer) MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().getCustomPlayer(player.getUniqueId());
        Pair<SkillType, Double> pair = SpecializationConfig.getXpGainFromCraftingConfig().get(crafted.getType(), new TypeToken<>() {});
        customPlayer.addSkillXp(pair.firstValue(), pair.secondValue() * event.getCurrentItem().getAmount());
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
        // Get the player viewing the crafting inventory
        for (var viewer : event.getInventory().getViewers()) {
            if (viewer instanceof Player) {
                player = (Player) viewer;
                break;
            }
        }
        
        if (player == null) return;
        
        Recipe recipe = event.getRecipe();
        if (recipe == null) return;
        
        // Cast to Keyed to access getKey() method
        if (!(recipe instanceof Keyed)) {
            LOGGER.warning("Recipe is not Keyed, cannot get NamespacedKey: " + recipe.getClass().getSimpleName());
            return;
        }
        
        NamespacedKey recipeKey = ((Keyed) recipe).getKey();
        
        // Check if this recipe should be blocked based on skill requirements
        if (shouldBlockRecipe(player, recipeKey)) {
            LOGGER.info("Blocking recipe " + recipeKey + " for player " + player.getName() + " due to insufficient skill level");
            // Clear the result to prevent crafting
            event.getInventory().setResult(null);
        } else {
            // Recipe is allowed - discover it on-demand if player doesn't have it
            if (!player.hasDiscoveredRecipe(recipeKey)) {
                player.discoverRecipe(recipeKey);
                LOGGER.fine("Discovered recipe " + recipeKey + " for player " + player.getName() + " on-demand");
            }
        }
    }

    /**
     * Simplified recipe blocking logic:
     * - If recipe is NOT in unlockedRecipesConfig, it's allowed (return false)
     * - If recipe IS in unlockedRecipesConfig, check if player meets skill requirements
     * - Block only if recipe is skill-locked and player doesn't meet requirements
     */
    private boolean shouldBlockRecipe(Player player, NamespacedKey recipeKey) {
        CustomPlayer customPlayer = (CustomPlayer) MinecraftCivilizationsCore.getInstance()
                .getCustomPlayerManager().getCustomPlayer(player.getUniqueId());
        
        // Check if recipe is in any skill-specific unlocked recipes config
        for (SkillType skillType : SkillType.values()) {
            for (SkillLevel skillLevel : SkillLevel.values()) {
                String configKey = skillType + "_" + skillLevel;
                Set<NamespacedKey> skillRecipes = SpecializationConfig.getUnlockedRecipesConfig()
                        .get(configKey, new TypeToken<>() {
                        });
                
                if (skillRecipes != null && skillRecipes.contains(recipeKey)) {
                    // Recipe is in skill config - check if player meets requirement
                    // Player meets requirement - allow recipe
                    // Player doesn't meet requirement - block recipe
                    return customPlayer.getSkillLevel(skillType) < skillLevel.ordinal();
                }
            }
        }
        
        // Recipe is not in any skill config - allow it (not skill-locked)
        return false;
    }
}
