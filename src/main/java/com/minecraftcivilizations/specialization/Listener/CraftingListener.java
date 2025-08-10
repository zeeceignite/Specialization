package com.minecraftcivilizations.specialization.Listener;

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
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRecipeDiscoverEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.HashSet;
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
     * This prevents unauthorized recipes from being shown to players.
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
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onRecipeDiscover(PlayerRecipeDiscoverEvent event) {
        Player player = event.getPlayer();
        NamespacedKey recipeKey = event.getRecipe();
        
        // Check if this recipe should be locked based on skill requirements
        if (shouldBlockRecipe(player, recipeKey)) {
            LOGGER.info("Blocking recipe " + recipeKey + " for player " + player.getName() + " due to insufficient skill level");
            event.setCancelled(true);
            // Also undiscover the recipe if they somehow already have it
            if (player.hasDiscoveredRecipe(recipeKey)) {
                player.undiscoverRecipe(recipeKey);
            }
        }
    }

    /**
     * Apply basic default recipes when player joins (lightweight approach).
     * Only applies recipes that are not skill-restricted.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Run after a short delay to ensure player data is fully loaded
        Bukkit.getScheduler().runTaskLater(MinecraftCivilizationsCore.getInstance(), () -> {
            applyBasicRecipes(player);
        }, 20L); // 1 second delay
    }

    /**
     * Checks if a player should be blocked from accessing a recipe based on skill requirements.
     * Prioritizes unlockedRecipesConfig over defaultUnlockedRecipesConfig for overlaps.
     */
    private boolean shouldBlockRecipe(Player player, NamespacedKey recipeKey) {
        CustomPlayer customPlayer = (CustomPlayer) MinecraftCivilizationsCore.getInstance()
                .getCustomPlayerManager().getCustomPlayer(player.getUniqueId());
        
        // Get default unlocked recipes
        Set<NamespacedKey> defaultRecipes = SpecializationConfig.getDefaultUnlockedRecipesConfig()
                .get("DEFAULT_UNLOCKED_RECIPES", new TypeToken<>() {
                });
        if (defaultRecipes == null) {
            defaultRecipes = new HashSet<>();
        }
        
        // Check if recipe is in any skill-specific unlocked recipes config
        boolean isInSkillConfig = false;
        boolean meetsSkillRequirement = false;
        
        for (SkillType skillType : SkillType.values()) {
            for (SkillLevel skillLevel : SkillLevel.values()) {
                String configKey = skillType + "_" + skillLevel;
                Set<NamespacedKey> skillRecipes = SpecializationConfig.getUnlockedRecipesConfig()
                        .get(configKey, new TypeToken<Set<NamespacedKey>>() {});
                
                if (skillRecipes != null && skillRecipes.contains(recipeKey)) {
                    isInSkillConfig = true;
                    
                    // Check if player meets the skill requirement
                    if (customPlayer.getSkillLevel(skillType) >= skillLevel.ordinal()) {
                        meetsSkillRequirement = true;
                        break;
                    }
                }
            }
            if (meetsSkillRequirement) break;
        }
        
        // If recipe is in skill config but player doesn't meet requirements, block access
        // This takes priority over defaultRecipes config (handles overlaps)
        if (isInSkillConfig && !meetsSkillRequirement) {
            return true;
        }
        
        // If recipe is in skill config and player meets requirements, allow access
        if (isInSkillConfig && meetsSkillRequirement) {
            return false;
        }
        
        // If recipe is not in skill config, check if it's in default recipes
        // Only block if it's not in default recipes
        if (!defaultRecipes.contains(recipeKey)) {
            return true;
        }
        return false;
    }

    /**
     * Apply only basic default recipes that are not restricted by skill requirements.
     * This is much more lightweight than the previous validatePlayerRecipes method.
     */
    private void applyBasicRecipes(Player player) {
        LOGGER.info("Applying basic recipes for player " + player.getName());
        
        // Get default unlocked recipes
        Set<NamespacedKey> defaultRecipes = SpecializationConfig.getDefaultUnlockedRecipesConfig()
                .get("DEFAULT_UNLOCKED_RECIPES", new TypeToken<>() {});
        if (defaultRecipes == null) {
            defaultRecipes = new HashSet<>();
        }
        
        // Get all skill-restricted recipes to exclude them from defaults
        Set<NamespacedKey> skillRestrictedRecipes = new HashSet<>();
        for (SkillType skillType : SkillType.values()) {
            for (SkillLevel skillLevel : SkillLevel.values()) {
                String configKey = skillType + "_" + skillLevel;
                Set<NamespacedKey> skillRecipes = SpecializationConfig.getUnlockedRecipesConfig()
                        .get(configKey, new TypeToken<Set<NamespacedKey>>() {});
                
                if (skillRecipes != null) {
                    skillRestrictedRecipes.addAll(skillRecipes);
                }
            }
        }
        
        // Apply only default recipes that are not skill-restricted
        int appliedCount = 0;
        for (NamespacedKey recipe : defaultRecipes) {
            if (!skillRestrictedRecipes.contains(recipe) && !player.hasDiscoveredRecipe(recipe)) {
                player.discoverRecipe(recipe);
                appliedCount++;
            }
        }
        
        LOGGER.info("Applied " + appliedCount + " basic recipes for player " + player.getName());
    }
}
