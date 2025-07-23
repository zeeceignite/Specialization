package com.minecraftcivilizations.specialization.Recipe;

import com.minecraftcivilizations.specialization.Config.Config;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.Skill;
import com.minecraftcivilizations.specialization.Specialization;
import lombok.Getter;
import lombok.NoArgsConstructor;
import minecraftcivilizations.com.minecraftCivilizationsCore.MinecraftCivilizationsCore;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.Iterator;
import java.util.UUID;

@NoArgsConstructor
public class RecipeManager {
    @Getter
    private static final RecipeManager instance = new RecipeManager();

    public void reloadPlayerRecipes(Player player) {
        CustomPlayer customPlayer = (CustomPlayer) MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().getCustomPlayer(player.getUniqueId());
        for (Skill skill : customPlayer.getSkills()) {
            switch (skill.getSkillLevel()) {
                case EXPERT:
                    lockAllRecipes(player.getUniqueId());
                    for ()

            }
        }
    }

    private void reloadRecipes(CustomPlayer customPlayer) {

    }

    public void reloadPlayerRecipes(CustomPlayer customPlayer) {

    }

    public void lockAllRecipes(UUID uuid) {
        for (Iterator<Recipe> it = Bukkit.recipeIterator(); it.hasNext(); ) {
            Recipe recipe = it.next();
            if (recipe instanceof Keyed keyed) {
                Bukkit.getPlayer(uuid).undiscoverRecipe(keyed.getKey());
            }
        }
    }



    public void unlockRecipe(UUID uuid, Recipe recipe) {
        if (recipe instanceof Keyed keyed) {
            Bukkit.getPlayer(uuid).discoverRecipe(keyed.getKey());
        }
    }

    public void unlockRecipe(UUID uuid, ItemStack item) {
        if (item == null) return;
        Recipe recipe = Bukkit.getRecipesFor(item).getFirst();
        if (recipe instanceof Keyed keyed) {
            Bukkit.getPlayer(uuid).discoverRecipe(keyed.getKey());
        }
    }

    public void unlockRecipes(UUID uuid, ItemStack... items) {
        for (ItemStack item : items) {
            if (item == null) return;
            unlockRecipe(uuid, item);
        }
    }

    public void lockRecipe(UUID uuid, Recipe recipe) {
        if (recipe instanceof Keyed keyed) {
            Bukkit.getPlayer(uuid).undiscoverRecipe(keyed.getKey());
        }
    }

    public void lockRecipe(UUID uuid, ItemStack item) {
        if (item == null) return;
        Recipe recipe = Bukkit.getRecipesFor(item).getFirst();
        if (recipe instanceof Keyed keyed) {
            Bukkit.getPlayer(uuid).undiscoverRecipe(keyed.getKey());
        }
    }

    public void lockRecipes(UUID uuid, ItemStack... items) {
        for (ItemStack item : items) {
            lockRecipe(uuid, item);
        }
    }

}
