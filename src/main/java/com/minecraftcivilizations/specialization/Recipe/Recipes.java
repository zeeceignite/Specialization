package com.minecraftcivilizations.specialization.Recipe;

import com.minecraftcivilizations.specialization.Specialization;
import minecraftcivilizations.com.minecraftCivilizationsCore.Item.CustomItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.ArrayList;

public class Recipes {
    public static void init() {
        ItemStack itemStack = CustomItem.newCustomItem(Material.PAPER,

                Component.text("Bandage"),
                new ArrayList<>() {
                    {
                        add(Component.empty());
                        add(Component.text("If used by a healer able to heal players"));
                    }
                },
                Specialization.getInstance());

        ShapelessRecipe shapelessRecipe = new ShapelessRecipe(new NamespacedKey(Specialization.getInstance(), "bandage"), itemStack);
        shapelessRecipe.addIngredient(new ItemStack(Material.PAPER, 8));
        shapelessRecipe.addIngredient(new ItemStack(Material.SUGAR_CANE));

        Bukkit.addRecipe(shapelessRecipe, true);

        itemStack = CustomItem.newCustomItem(Material.PAPER,

                Component.text("Iron Chestplate Blueprint").color(NamedTextColor.WHITE),
                new ArrayList<>() {
                    {
                        add(Component.empty());
                        add(Component.text("Used for crafting of").color(NamedTextColor.WHITE));
                        add(Component.text("the Iron Chestplate").color(NamedTextColor.WHITE));
                        add(Component.empty());
                        add(Component.text("Crafted by the most").color(NamedTextColor.GRAY));
                        add(Component.text("esteemed of librarians").color(NamedTextColor.GRAY));
                    }
                },
                Specialization.getInstance());

        shapelessRecipe = new ShapelessRecipe(new NamespacedKey(Specialization.getInstance(), "iron_chestplate_blueprint"), itemStack);
        shapelessRecipe.addIngredient(new ItemStack(Material.PAPER, 8));
        shapelessRecipe.addIngredient(new ItemStack(Material.IRON_INGOT));

        Bukkit.addRecipe(shapelessRecipe, true);
    }
}
