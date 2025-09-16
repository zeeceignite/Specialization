package com.minecraftcivilizations.specialization.GUI;

import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillLevel;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import com.minecraftcivilizations.specialization.util.LoreUtils;
import minecraftcivilizations.com.minecraftCivilizationsCore.GUI.GUI;
import minecraftcivilizations.com.minecraftCivilizationsCore.GUI.GUIItem;
import minecraftcivilizations.com.minecraftCivilizationsCore.Item.ItemUtils;
import minecraftcivilizations.com.minecraftCivilizationsCore.Options.GUIPlaceOption;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class RecipesGUI extends GUI {
    public SkillType skillType;
    public CustomPlayer customPlayer;
    public RecipesGUI(CustomPlayer customPlayer, SkillType skillType) {
        super(Component.text(skillType != null ? "Unlocked Recipes in " + SkillType.getDisplayName(skillType) : "Choose a Recipe SkillTree To View").color(NamedTextColor.BLACK), 54, new HashMap<>() {
            {
                put(45, new GUIItem(ItemUtils.makeItemGUIItem(ItemStack.of(Material.ARROW), "Back to Class Menu").getItem(), () -> {
                    new ClassGUI().open(Bukkit.getPlayer(customPlayer.getUuid()));
                }));
            }
        }, new HashMap<>() {
            {
                put(GUIPlaceOption.SHOULD_PLACE_EXIT, true);
                put(GUIPlaceOption.SHOULD_PLACE_BACK, true);
            }
        });
        this.skillType = skillType;
        this.customPlayer = customPlayer;
    }

    @Override
    public void open(Player player) {
        for (int i = 0; i < SkillType.values().length; i++) {
            SkillType skillType1 = SkillType.values()[i];
            GUIItem put = new GUIItem(ItemUtils.makeItemGUIItem(ItemStack.of(skillType1.getSkillWorkstation()), SkillType.getDisplayName(skillType1)).getItem(), () -> {
                if (skillType == skillType1) {
                    return;
                }
                new RecipesGUI(customPlayer, skillType1).setParentGUI(RecipesGUI.this).open(Bukkit.getPlayer(customPlayer.getUuid()));
            });
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
        getItems().put(21, ItemUtils.makeItemGUIItem(ItemStack.of(Material.YELLOW_STAINED_GLASS_PANE), "Your path"));
        getItems().put(22, ItemUtils.makeItemGUIItem(ItemStack.of(Material.YELLOW_STAINED_GLASS_PANE), "Your path"));
        getItems().put(23, ItemUtils.makeItemGUIItem(ItemStack.of(Material.YELLOW_STAINED_GLASS_PANE), "Your path"));
        if (customPlayer.getSkillLevel(skillType) >= 1) {
            getItems().put(18, ItemUtils.makeItemGUIItem(ItemStack.of(Material.LIME_STAINED_GLASS_PANE), SkillLevel.getDisplayName(1) + " Unlocked"));
            getItems().put(27, ItemUtils.makeItemGUIItem(ItemStack.of(Material.LIME_STAINED_GLASS_PANE), SkillLevel.getDisplayName(1) + " Unlocked"));
            guiItem = ItemUtils.makeItemGUIItem(ItemStack.of(Material.WRITABLE_BOOK), SkillLevel.getDisplayName(1) + " Unlocked");
            guiItem.getItem().editMeta(itemMeta -> {
                itemMeta.lore(LoreUtils.createDescriptionLoreLine("Click to view recipes you've unlocked"));
            });
            getItems().put(36, guiItem);
        } else {
            getItems().put(18, ItemUtils.makeItemGUIItem(ItemStack.of(Material.RED_STAINED_GLASS_PANE), SkillLevel.getDisplayName(1) + " Not Unlocked"));
            getItems().put(27, ItemUtils.makeItemGUIItem(ItemStack.of(Material.RED_STAINED_GLASS_PANE), SkillLevel.getDisplayName(1) + " Not Unlocked"));
            guiItem = ItemUtils.makeItemGUIItem(ItemStack.of(Material.WRITABLE_BOOK), SkillLevel.getDisplayName(1) + " Not Unlocked");
            guiItem.getItem().editMeta(itemMeta -> {
                itemMeta.lore(LoreUtils.createDescriptionLoreLine("Click to view recipes you'll unlock"));
            });
            getItems().put(36, guiItem);
        }
        if (customPlayer.getSkillLevel(skillType) >= 2) {
            getItems().put(20, ItemUtils.makeItemGUIItem(ItemStack.of(Material.LIME_STAINED_GLASS_PANE), SkillLevel.getDisplayName(2) + " Unlocked"));
            getItems().put(29, ItemUtils.makeItemGUIItem(ItemStack.of(Material.LIME_STAINED_GLASS_PANE), SkillLevel.getDisplayName(2) + " Unlocked"));
            guiItem = ItemUtils.makeItemGUIItem(ItemStack.of(Material.WRITABLE_BOOK), SkillLevel.getDisplayName(1) + " Unlocked");
            guiItem.getItem().editMeta(itemMeta -> {
                itemMeta.lore(LoreUtils.createDescriptionLoreLine("Click to view recipes you've unlocked"));
            });
            getItems().put(38, guiItem);
        } else {
            getItems().put(20, ItemUtils.makeItemGUIItem(ItemStack.of(Material.RED_STAINED_GLASS_PANE), SkillLevel.getDisplayName(2) + " Not Unlocked"));
            getItems().put(29, ItemUtils.makeItemGUIItem(ItemStack.of(Material.RED_STAINED_GLASS_PANE), SkillLevel.getDisplayName(2) + " Not Unlocked"));
            if (customPlayer.getSkillLevel(skillType) < 1) {
                guiItem = ItemUtils.makeItemGUIItem(ItemStack.of(Material.BOOK), SkillLevel.getDisplayName(1) + " Not Unlocked");
                guiItem.getItem().editMeta(itemMeta -> {
                    itemMeta.lore(LoreUtils.createDescriptionLoreLine("Click to view recipes you've unlocked"));
                });
                getItems().put(38, guiItem);
            }
        }
        if (customPlayer.getSkillLevel(skillType) >= 3) {
            getItems().put(31, ItemUtils.makeItemGUIItem(ItemStack.of(Material.LIME_STAINED_GLASS_PANE), SkillLevel.getDisplayName(3) + " Unlocked"));
            guiItem = ItemUtils.makeItemGUIItem(ItemStack.of(Material.WRITABLE_BOOK), SkillLevel.getDisplayName(1) + " Unlocked");
            guiItem.getItem().editMeta(itemMeta -> {
                itemMeta.lore(LoreUtils.createDescriptionLoreLine("Click to view recipes you've unlocked"));
            });
            getItems().put(40, guiItem);
        } else {
            getItems().put(31, ItemUtils.makeItemGUIItem(ItemStack.of(Material.RED_STAINED_GLASS_PANE), SkillLevel.getDisplayName(3) + " Not Unlocked"));
        }
        if (customPlayer.getSkillLevel(skillType) >= 4) {
            getItems().put(24, ItemUtils.makeItemGUIItem(ItemStack.of(Material.LIME_STAINED_GLASS_PANE), SkillLevel.getDisplayName(4) + " Unlocked"));
            getItems().put(33, ItemUtils.makeItemGUIItem(ItemStack.of(Material.LIME_STAINED_GLASS_PANE), SkillLevel.getDisplayName(4) + " Unlocked"));
            guiItem = ItemUtils.makeItemGUIItem(ItemStack.of(Material.WRITABLE_BOOK), SkillLevel.getDisplayName(1) + " Unlocked");
            guiItem.getItem().editMeta(itemMeta -> {
                itemMeta.lore(LoreUtils.createDescriptionLoreLine("Click to view recipes you've unlocked"));
            });
            getItems().put(42, guiItem);
        } else {
            getItems().put(24, ItemUtils.makeItemGUIItem(ItemStack.of(Material.RED_STAINED_GLASS_PANE), SkillLevel.getDisplayName(4) + " Not Unlocked"));
            getItems().put(33, ItemUtils.makeItemGUIItem(ItemStack.of(Material.RED_STAINED_GLASS_PANE), SkillLevel.getDisplayName(4) + " Not Unlocked"));
        }
        if (customPlayer.getSkillLevel(skillType) >= 5) {
            getItems().put(26, ItemUtils.makeItemGUIItem(ItemStack.of(Material.LIME_STAINED_GLASS_PANE), SkillLevel.getDisplayName(5) + " Unlocked"));
            getItems().put(35, ItemUtils.makeItemGUIItem(ItemStack.of(Material.LIME_STAINED_GLASS_PANE), SkillLevel.getDisplayName(5) + " Unlocked"));
            guiItem = ItemUtils.makeItemGUIItem(ItemStack.of(Material.WRITABLE_BOOK), SkillLevel.getDisplayName(1) + " Unlocked");
            guiItem.getItem().editMeta(itemMeta -> {
                itemMeta.lore(LoreUtils.createDescriptionLoreLine("Click to view recipes you've unlocked"));
            });
            getItems().put(44, guiItem);
        } else {
            getItems().put(26, ItemUtils.makeItemGUIItem(ItemStack.of(Material.RED_STAINED_GLASS_PANE), SkillLevel.getDisplayName(5) + " Not Unlocked"));
            getItems().put(35, ItemUtils.makeItemGUIItem(ItemStack.of(Material.RED_STAINED_GLASS_PANE), SkillLevel.getDisplayName(5) + " Not Unlocked"));
        }
        super.open(player);
    }

}
