package com.minecraftcivilizations.specialization.Listener;

import com.google.gson.reflect.TypeToken;
import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillLevel;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import minecraftcivilizations.com.minecraftCivilizationsCore.MinecraftCivilizationsCore;
import minecraftcivilizations.com.minecraftCivilizationsCore.Options.Pair;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRecipeDiscoverEvent;
import org.bukkit.inventory.ItemStack;

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
     * Validate player recipes when they join the server
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Run validation after a short delay to ensure player data is fully loaded
        Bukkit.getScheduler().runTaskLater(MinecraftCivilizationsCore.getInstance(), () -> {
            validatePlayerRecipes(player);
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
     * Actively manages player recipe discovery on join.
     * Priority: unlockedRecipesConfig > defaultUnlockedRecipesConfig
     * Process: 1) Undiscover all recipes, 2) Check skill configs, 3) Add allowed defaults
     */
    private void validatePlayerRecipes(Player player) {
        LOGGER.info("Starting recipe validation for player " + player.getName());
        
        CustomPlayer customPlayer = (CustomPlayer) MinecraftCivilizationsCore.getInstance()
                .getCustomPlayerManager().getCustomPlayer(player.getUniqueId());
        
        // Step 1: Get all currently discovered recipes and undiscover them
        Set<NamespacedKey> currentRecipes = new HashSet<>(player.getDiscoveredRecipes());
        LOGGER.info("Player " + player.getName() + " currently has " + currentRecipes.size() + " discovered recipes");
        
        // Undiscover all current recipes
        for (NamespacedKey recipe : currentRecipes) {
            player.undiscoverRecipe(recipe);
        }
        LOGGER.info("Undiscovered all recipes for player " + player.getName());
        
        // Step 2: Check unlocked recipes config first (PRIORITY)
        Set<NamespacedKey> allowedRecipes = new HashSet<>();
        
        for (SkillType skillType : SkillType.values()) {
            for (SkillLevel skillLevel : SkillLevel.values()) {
                String configKey = skillType + "_" + skillLevel;
                Set<NamespacedKey> skillRecipes = SpecializationConfig.getUnlockedRecipesConfig()
                        .get(configKey, new TypeToken<>() {
                        });
                
                if (skillRecipes != null) {
                    // Check if player meets the skill requirement
                    if (customPlayer.getSkillLevel(skillType) >= skillLevel.ordinal()) {
                        allowedRecipes.addAll(skillRecipes);
                    }
                }
            }
        }
        
        // Step 3: Get default unlocked recipes
        Set<NamespacedKey> defaultRecipes = SpecializationConfig.getDefaultUnlockedRecipesConfig()
                .get("DEFAULT_UNLOCKED_RECIPES", new TypeToken<>() {});
        if (defaultRecipes == null) {
            defaultRecipes = new HashSet<>();
        }
        
        // Step 4: Add default recipes that are NOT already in skill configs (avoid conflicts)
        Set<NamespacedKey> finalAllowedRecipes = new HashSet<>(allowedRecipes);
        int defaultsAdded = 0;
        for (NamespacedKey defaultRecipe : defaultRecipes) {
            if (!allowedRecipes.contains(defaultRecipe)) {
                finalAllowedRecipes.add(defaultRecipe);
                defaultsAdded++;
            }
        }
        
        LOGGER.info("Added " + defaultsAdded + " default recipes (non-conflicting) for player " + player.getName());
        LOGGER.info("Total allowed recipes for player " + player.getName() + ": " + finalAllowedRecipes.size());
        
        // Step 5: Discover all allowed recipes
        for (NamespacedKey recipe : finalAllowedRecipes) {
            player.discoverRecipe(recipe);
        }
        
        LOGGER.info("Recipe validation completed for player " + player.getName() + 
                   ". Discovered " + finalAllowedRecipes.size() + " recipes");
    }
}
