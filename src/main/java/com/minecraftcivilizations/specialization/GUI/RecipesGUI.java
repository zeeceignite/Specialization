package com.minecraftcivilizations.specialization.GUI;

import com.google.gson.reflect.TypeToken;
import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillLevel;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import minecraftcivilizations.com.minecraftCivilizationsCore.GUI.GUIItem.GUIItem;
import minecraftcivilizations.com.minecraftCivilizationsCore.GUI.GUIPlaceOption;
import minecraftcivilizations.com.minecraftCivilizationsCore.GUI.GUIPlacement;
import minecraftcivilizations.com.minecraftCivilizationsCore.GUI.GUIs.GUI;
import minecraftcivilizations.com.minecraftCivilizationsCore.GUI.GUIs.ListGUI;
import minecraftcivilizations.com.minecraftCivilizationsCore.Util.ItemUtils;
import minecraftcivilizations.com.minecraftCivilizationsCore.Util.LoreUtils;
import minecraftcivilizations.com.minecraftCivilizationsCore.Util.SearchUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class RecipesGUI extends GUI {
    public SkillType skillType;
    public CustomPlayer customPlayer;
    public RecipesGUI(CustomPlayer customPlayer, SkillType skillType) {
        super(Component.text(skillType != null ? "Unlocked Recipes in " + SkillType.getDisplayName(skillType) : "Choose Recipe SkillTree To View").color(NamedTextColor.BLACK), 54, new HashMap<>() {
            {
                put(45, ItemUtils.makeItemGUIItem(ItemStack.of(Material.ARROW), "Back to Class Menu")
                        .addOnClick(() -> new ClassGUI().open(Bukkit.getPlayer(customPlayer.getUuid())),  ClickType.UNKNOWN));
            }
        }, new HashMap<>() {
            {
                put(GUIPlaceOption.SHOULD_PLACE_EXIT, GUIPlacement.of(true));
                put(GUIPlaceOption.SHOULD_PLACE_BACK, GUIPlacement.of(true));
            }
        });
        this.skillType = skillType;
        this.customPlayer = customPlayer;
    }

    @Override
    public void open(Player player) {
        for (int i = 0; i < SkillType.values().length; i++) {
            SkillType skillType1 = SkillType.values()[i];
            GUIItem put = ItemUtils.makeItemGUIItem(ItemStack.of(skillType1.getSkillWorkstation()), SkillType.getDisplayName(skillType1))
                    .addOnClick(() -> {
                        if (skillType == skillType1) {
                            return;
                        }
                        new RecipesGUI(customPlayer, skillType1).setParentGUI(RecipesGUI.this).open(Bukkit.getPlayer(customPlayer.getUuid()));
                    }, ClickType.UNKNOWN);
            put.getItem().editMeta(itemMeta -> {
                itemMeta.lore(LoreUtils.createDescriptionLoreLine(skillType1.getSkillDescription()));
            });
            getItems().put(i + 1, put);
        }
        if (skillType == null) {
            super.open(player);
            return;
        }
        for (int i = 9; i < 18; i++) {
            getItems().put(i, ItemUtils.makeItemGUIItem(ItemStack.of(Material.YELLOW_STAINED_GLASS_PANE), "Your path"));
        }
        GUIItem guiItem = ItemUtils.makeGUIItemOfType(skillType.getSkillWorkstation(), SkillType.getDisplayName(skillType));
        guiItem.getItem().editMeta(itemMeta -> {
            itemMeta.lore(LoreUtils.createDescriptionLoreLine(skillType.getSkillDescription()));
        });
        getItems().put(13, guiItem);
        if (!customPlayer.getPlayerOptions().isNewRecipeGUIIteration()) {
            getItems().put(21, ItemUtils.makeItemGUIItem(ItemStack.of(Material.YELLOW_STAINED_GLASS_PANE), "Your path"));
            getItems().put(22, ItemUtils.makeItemGUIItem(ItemStack.of(Material.YELLOW_STAINED_GLASS_PANE), "Your path"));
            getItems().put(23, ItemUtils.makeItemGUIItem(ItemStack.of(Material.YELLOW_STAINED_GLASS_PANE), "Your path"));
        }

        getItems().put(18, recipeItem(1));
        getItems().put(27, recipeItem(1));
        getItems().put(36, viewRecipesItem(1));

        getItems().put(20, recipeItem(2));
        getItems().put(29, recipeItem(2));
        getItems().put(38, viewRecipesItem(2));

        if (customPlayer.getPlayerOptions().isNewRecipeGUIIteration()) getItems().put(22, recipeItem(3));
        getItems().put(31, recipeItem(3));
        getItems().put(40, viewRecipesItem(3));

        getItems().put(24, recipeItem(4));
        getItems().put(33, recipeItem(4));
        getItems().put(42, viewRecipesItem(4));

        getItems().put(26, recipeItem(5));
        getItems().put(35, recipeItem(5));
        getItems().put(44, viewRecipesItem(5));

        super.open(player);
    }

    public GUIItem recipeItem(int level) {
        if (customPlayer.getSkillLevel(skillType) >= level) {
            return ItemUtils.makeItemGUIItem(ItemStack.of(Material.LIME_STAINED_GLASS_PANE), SkillLevel.getDisplayName(level) + " Unlocked");
        }
        return ItemUtils.makeItemGUIItem(ItemStack.of(Material.RED_STAINED_GLASS_PANE), SkillLevel.getDisplayName(level) + " Not Unlocked");
    }

    public GUIItem viewRecipesItem(int requiredLevel) {
        GUIItem guiItem;
        if (customPlayer.getSkillLevel(skillType) < requiredLevel - 1) {
            guiItem = ItemUtils.makeItemGUIItem(ItemStack.of(Material.BOOK), SkillLevel.getDisplayName(requiredLevel) + " Not Unlocked");
            guiItem.getItem().editMeta(itemMeta -> itemMeta.lore(LoreUtils.createDescriptionLoreLine("You can't view recipes yet, you'll be able to see it once you're one level under the requirement (" + (requiredLevel - 1) + ")")));
            return guiItem;
        } else if (customPlayer.getSkillLevel(skillType) == requiredLevel - 1) {
            guiItem = ItemUtils.makeItemGUIItem(ItemStack.of(Material.BOOK), SkillLevel.getDisplayName(requiredLevel) + " Not Unlocked");
            guiItem.getItem().editMeta(itemMeta -> itemMeta.lore(LoreUtils.createDescriptionLoreLine("Click to view recipes you'll unlock")));
            guiItem.addOnClick(() -> {
                Set<NamespacedKey> stringHashSetPair = SpecializationConfig.getUnlockedRecipesConfig().get(skillType + "_" + SkillLevel.getSkillLevelFromInt(requiredLevel), new TypeToken<>() {
                });
                ArrayList<ItemStack> itemStacks = new ArrayList<>(0);
                if (stringHashSetPair != null) {
                    for (NamespacedKey namespacedKey : stringHashSetPair) {
                        itemStacks.add(ItemStack.of(Registry.MATERIAL.get(namespacedKey)));
                    }
                    HashMap<GUIPlaceOption, GUIPlacement> map = new HashMap<>(0);
                    map.putAll(Map.of(
                            GUIPlaceOption.SHOULD_PLACE_EXIT, GUIPlacement.of(false),
                            GUIPlaceOption.SHOULD_PLACE_BACK, GUIPlacement.of(true),
                            GUIPlaceOption.SHOULD_PLACE_BACK_TO_DIFFERENT_MENU, GUIPlacement.of(true),
                            GUIPlaceOption.SHOULD_PLACE_SEARCH, GUIPlacement.of(null, true, s -> SearchUtils.searchFromProvidedItems(s, itemStacks))
                    ));
                    new ListGUI(Component.text("Recipes"), 54, map, itemStacks).setParentGUI(this).open(Bukkit.getPlayer(customPlayer.getUuid()));
                }
            });
            return guiItem;
        }
        guiItem = ItemUtils.makeItemGUIItem(ItemStack.of(Material.WRITABLE_BOOK), SkillLevel.getDisplayName(requiredLevel) + " Unlocked");
        guiItem.getItem().editMeta(itemMeta -> {
            itemMeta.lore(LoreUtils.createDescriptionLoreLine("Click to view recipes you've unlocked"));
        });
        guiItem.addOnClick(() -> {
            Set<NamespacedKey> stringHashSetPair = SpecializationConfig.getUnlockedRecipesConfig().get(skillType + "_" + SkillLevel.getSkillLevelFromInt(requiredLevel), new TypeToken<>() {
            });
            ArrayList<ItemStack> itemStacks = new ArrayList<>(0);
            if (stringHashSetPair != null) {
                for (NamespacedKey namespacedKey : stringHashSetPair) {
                    itemStacks.add(ItemStack.of(Registry.MATERIAL.get(namespacedKey)));
                }
                new ListGUI(Component.text("Recipes"), 54, itemStacks, Map.of(GUIPlaceOption.SHOULD_PLACE_EXIT, false, GUIPlaceOption.SHOULD_PLACE_BACK, true, GUIPlaceOption.SHOULD_PLACE_SEARCH, false)).setParentGUI(this).open(Bukkit.getPlayer(customPlayer.getUuid()));
            }
        });
        return guiItem;
    }
}
