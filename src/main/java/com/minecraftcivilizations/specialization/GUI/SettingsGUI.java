package com.minecraftcivilizations.specialization.GUI;

import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import minecraftcivilizations.com.minecraftCivilizationsCore.GUI.GUIItem.GUIItem;
import minecraftcivilizations.com.minecraftCivilizationsCore.GUI.GUIPlaceOption;
import minecraftcivilizations.com.minecraftCivilizationsCore.GUI.GUIPlacement;
import minecraftcivilizations.com.minecraftCivilizationsCore.GUI.GUIs.GUI;
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

import java.util.Map;

public class SettingsGUI extends GUI {
    public SettingsGUI() {
        super(Component.text("Settings").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false), 27,
                Map.of(GUIPlaceOption.SHOULD_PLACE_EXIT, GUIPlacement.of(false), GUIPlaceOption.SHOULD_PLACE_BACK, GUIPlacement.of(true)));

    }

    @Override
    public void open(Player player) {
        ItemStack settings;
        CustomPlayer customPlayer = (CustomPlayer) MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().getCustomPlayer(player.getUniqueId());
        if (customPlayer.getPlayerOptions().isAdvancedClassesGUIEnabled()) {
            settings = ItemStack.of(Material.GREEN_DYE);
        } else {
            settings = ItemStack.of(Material.RED_DYE);
        }
        ItemMeta settingsItemMeta = settings.getItemMeta();
        settingsItemMeta.addItemFlags(ItemFlag.values());
        settingsItemMeta.displayName(Component.text("Enable/Disable Advanced Class GUI").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.WHITE));
        settings.setItemMeta(settingsItemMeta);
        this.getItems().put(10, new GUIItem(settings)
                .addOnClick(() -> {
            customPlayer.getPlayerOptions().setAdvancedClassesGUIEnabled(!customPlayer.getPlayerOptions().isAdvancedClassesGUIEnabled());
            getItems().clear();
            SettingsGUI.this.open(player);
        }, ClickType.UNKNOWN));

        if (customPlayer.getPlayerOptions().isNewRecipeGUIIteration()) {
            settings = ItemStack.of(Material.GREEN_DYE);
        } else {
            settings = ItemStack.of(Material.RED_DYE);
        }
        settingsItemMeta = settings.getItemMeta();
        settingsItemMeta.addItemFlags(ItemFlag.values());
        settingsItemMeta.displayName(Component.text("Enable/Disable The Different Recipes GUI").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.WHITE));
        settings.setItemMeta(settingsItemMeta);
        this.getItems().put(11, new GUIItem(settings)
                .addOnClick(() -> {
            customPlayer.getPlayerOptions().setNewRecipeGUIIteration(!customPlayer.getPlayerOptions().isNewRecipeGUIIteration());
            getItems().clear();
            SettingsGUI.this.open(player);
        }, ClickType.UNKNOWN));

        if (customPlayer.getPlayerOptions().isSoundEnabled()) {
            settings = ItemStack.of(Material.GREEN_DYE);
        } else {
            settings = ItemStack.of(Material.RED_DYE);
        }
        settingsItemMeta = settings.getItemMeta();
        settingsItemMeta.addItemFlags(ItemFlag.values());
        settingsItemMeta.displayName(Component.text("Enable/Disable XP gain sound").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.WHITE));
        settings.setItemMeta(settingsItemMeta);
        this.getItems().put(12, new GUIItem(settings)
                .addOnClick(() -> {
            customPlayer.getPlayerOptions().setSoundEnabled(!customPlayer.getPlayerOptions().isSoundEnabled());
            getItems().clear();
            SettingsGUI.this.open(player);
        }, ClickType.UNKNOWN));
        super.open(player);
    }
}
