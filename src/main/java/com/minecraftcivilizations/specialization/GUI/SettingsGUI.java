package com.minecraftcivilizations.specialization.GUI;

import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import minecraftcivilizations.com.minecraftCivilizationsCore.GUI.GUI.GUI;
import minecraftcivilizations.com.minecraftCivilizationsCore.GUI.GUI.ListGUI;
import minecraftcivilizations.com.minecraftCivilizationsCore.GUI.GUI.ScrollableHorizontalGUI;
import minecraftcivilizations.com.minecraftCivilizationsCore.GUI.GUI.SearchSignGUI;
import minecraftcivilizations.com.minecraftCivilizationsCore.GUI.GUIItem.GUIItem;
import minecraftcivilizations.com.minecraftCivilizationsCore.GUI.GUIPlaceOption;
import minecraftcivilizations.com.minecraftCivilizationsCore.GUI.GUIPlacement;
import minecraftcivilizations.com.minecraftCivilizationsCore.MinecraftCivilizationsCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SettingsGUI extends ScrollableHorizontalGUI {
    public SettingsGUI() {
        super(Component.text("Settings").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false), 27,
                List.of(),
                Map.of(GUIPlaceOption.SHOULD_PLACE_EXIT, GUIPlacement.of(false), GUIPlaceOption.SHOULD_PLACE_BACK, GUIPlacement.of(true)));

    }

    @Override
    public void open(Player player) {
        CustomPlayer customPlayer = CoreUtil.getPlayer(player);
        getVisibleItems().add(createItemFromBoolean(
                customPlayer.getPlayerOptions().isAdvancedClassesGUIEnabled(),
                Component.text("Enable/Disable Advanced Class GUI").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.WHITE))
                .addOnClick(() -> {
                    customPlayer.getPlayerOptions().setAdvancedClassesGUIEnabled(!customPlayer.getPlayerOptions().isAdvancedClassesGUIEnabled());
                    this.getVisibleItems().clear();
                    getItems().clear();
                    SettingsGUI.this.open(player);
                }, ClickType.UNKNOWN)
        );
        getVisibleItems().add(createItemFromBoolean(
                customPlayer.getPlayerOptions().isNewRecipeGUIIteration(),
                Component.text("Enable/Disable The Different Recipes GUI").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.WHITE))
                .addOnClick(() -> {
                    customPlayer.getPlayerOptions().setNewRecipeGUIIteration(!customPlayer.getPlayerOptions().isNewRecipeGUIIteration());
                    this.getVisibleItems().clear();
                    getItems().clear();
                    SettingsGUI.this.open(player);
                }, ClickType.UNKNOWN)
        );
        getVisibleItems().add(createItemFromBoolean(
                customPlayer.getPlayerOptions().isSoundEnabled(),
                Component.text("Enable/Disable XP gain sound").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.WHITE))
                .addOnClick(() -> {
                    customPlayer.getPlayerOptions().setSoundEnabled(!customPlayer.getPlayerOptions().isSoundEnabled());
                    this.getVisibleItems().clear();
                    getItems().clear();
                    SettingsGUI.this.open(player);
                }, ClickType.UNKNOWN)
        );
        getVisibleItems().add(createItemFromMaterial(
                Material.OAK_SIGN,
                Component.text("Set default background material").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.WHITE))
                .addOnClick(() -> {
                    ListGUI listGUI = new ListGUI(Component.text("Set default background material"), 54, Map.of(GUIPlaceOption.SHOULD_PLACE_EXIT, GUIPlacement.of(false), GUIPlaceOption.SHOULD_PLACE_BACK, GUIPlacement.of(true)), List.of());
                    listGUI.setParentGUI(this);
                    ArrayList<ItemStack> glassPane = SearchSignGUI.searchItemsAndBlocks("pane");
                    for (int i = 0; i < glassPane.size(); i++) {
                        int finalI = i;
                        listGUI.addItem(i, new GUIItem(glassPane.get(i))
                                .addOnClick(() -> {
                                    customPlayer.getPlayerOptions().setDefaultBackgroundMaterial(glassPane.get(finalI).getType());
                                    new SettingsGUI().setParentGUI(this.getParentGUI()).open(player);
                                }));

                    }
                    listGUI.open(player);
                }, ClickType.UNKNOWN)
        );

        super.open(player);
    }

    private GUIItem createItemFromBoolean(boolean value, Component name) {
        ItemStack settings;
        if (value) {
            settings = ItemStack.of(Material.GREEN_DYE);
        } else {
            settings = ItemStack.of(Material.RED_DYE);
        }
        ItemMeta settingsItemMeta = settings.getItemMeta();
        settingsItemMeta.addItemFlags(ItemFlag.values());
        settingsItemMeta.displayName(name);
        settings.setItemMeta(settingsItemMeta);
        return new GUIItem(settings);
    }

    private GUIItem createItemFromMaterial(Material material, Component name) {
        ItemStack settings;
        settings = ItemStack.of(material);
        ItemMeta settingsItemMeta = settings.getItemMeta();
        settingsItemMeta.addItemFlags(ItemFlag.values());
        settingsItemMeta.displayName(name);
        settings.setItemMeta(settingsItemMeta);
        return new GUIItem(settings);
    }
}
