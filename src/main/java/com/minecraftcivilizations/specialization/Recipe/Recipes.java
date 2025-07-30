package com.minecraftcivilizations.specialization.Recipe;

import com.minecraftcivilizations.specialization.Specialization;
import minecraftcivilizations.com.minecraftCivilizationsCore.Item.CustomItem;
import minecraftcivilizations.com.minecraftCivilizationsCore.Item.CustomItemRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.List;

public class Recipes {

    public static void init() {
        ItemStack itemStack = ItemStack.of(Material.PAPER);
        ShapelessRecipe shapelessRecipe = new ShapelessRecipe(new NamespacedKey(Specialization.getInstance(), "bandage"), itemStack);
        shapelessRecipe.addIngredient(new ItemStack(Material.PAPER, 8));
        shapelessRecipe.addIngredient(new ItemStack(Material.SUGAR_CANE));

        CustomItem newCustomItem = new CustomItem();
        newCustomItem.setItem(itemStack);

        CustomItemRegistry.register(new NamespacedKey(Specialization.getInstance(), "bandage"), newCustomItem);

        Bukkit.addRecipe(shapelessRecipe, true);
    }
}
