package com.minecraftcivilizations.specialization.GUI;

import minecraftcivilizations.com.minecraftCivilizationsCore.GUI.GUI.GUI;
import minecraftcivilizations.com.minecraftCivilizationsCore.GUI.GUIItem.GUIItem;
import minecraftcivilizations.com.minecraftCivilizationsCore.GUI.GUIPlaceOption;
import minecraftcivilizations.com.minecraftCivilizationsCore.GUI.GUIPlacement;
import minecraftcivilizations.com.minecraftCivilizationsCore.Util.ItemUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class ConfirmationGUI extends GUI {
    private final Runnable onConfirm;
    private final Runnable onCancel;

    public ConfirmationGUI(Runnable onConfirm, Runnable onCancel) {
        super(Component.text("Confirmation"), 27, Map.of(GUIPlaceOption.SHOULD_PLACE_EXIT, GUIPlacement.of(false)));
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
    }

    @Override
    public void open(Player player) {
        getItems().put(11, new GUIItem(Material.GREEN_CONCRETE, "Confirm")
                .addOnClick(onConfirm));
        getItems().put(15, new GUIItem(Material.RED_CONCRETE, "Cancel")
                .addOnClick(onCancel));
        super.open(player);
    }
}
