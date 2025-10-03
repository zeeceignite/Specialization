package com.minecraftcivilizations.specialization.GUI;

import minecraftcivilizations.com.minecraftCivilizationsCore.GUI.GUIItem.GUIItem;
import minecraftcivilizations.com.minecraftCivilizationsCore.GUI.GUIPlaceOption;
import minecraftcivilizations.com.minecraftCivilizationsCore.GUI.GUIPlacement;
import minecraftcivilizations.com.minecraftCivilizationsCore.GUI.GUIs.GUI;
import minecraftcivilizations.com.minecraftCivilizationsCore.GUI.GUIs.ScrollableVerticalGUI;
import minecraftcivilizations.com.minecraftCivilizationsCore.Options.Pair;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public class SkillTreeGUI extends ScrollableVerticalGUI {
    public SkillTreeGUI() {
        super(Component.text("Your SkillTree"), 54, items, placementOptions);
    }

    @Override
    public void open(Player player) {
        int itemIndex = 0;
        int rows = getSize() / 9 - 2;
        int offset = getScrollIndex() * rows;

        for (int row = 0; row < 7; row++) {
            for (int column = 0; column < rows; column++) {
                if (itemIndex + offset < getVisibleItems().size()) {
                    this.getItems().put(row + 10 + (column * 9), new GUIItem(getVisibleItems().get(itemIndex + offset)));
                } else {
                    this.getItems().remove(row + 10 + (column * 9));
                }
                itemIndex++;
            }
        }

        if (getScrollIndex() == 0) {
            if (getParentGUI() == null) {
                this.getItems().remove(placementOverride.get(GUIPlaceOption.SHOULD_PLACE_BACK).firstValue().slot());
                placementOverride.put(GUIPlaceOption.SHOULD_PLACE_BACK, Pair.of(GUIPlacement.of(false), null));
            } else {
                placementOverride.put(GUIPlaceOption.SHOULD_PLACE_BACK, Pair.of(GUIPlacement.of(
                                        placementOptions.containsKey(GUIPlaceOption.SHOULD_PLACE_EXIT) ? placementOptions.get(GUIPlaceOption.SHOULD_PLACE_BACK).slot() : getSize() - 9,
                                        true),
                                new GUIItem(Material.ARROW, "Back")
                                        .addOnClick(() -> back((Player) getInventory().getViewers().getFirst()), ClickType.UNKNOWN)
                                        .addOnClick(() -> back((Player) getInventory().getViewers().getFirst(), 7), ClickType.SHIFT_LEFT)
                        )
                );
            }
        }

        if (getVisibleItems().size() < (itemIndex + offset)) {
            this.getItems().remove(placementOverride.get(GUIPlaceOption.SHOULD_PLACE_NEXT).firstValue().slot());
            placementOverride.put(GUIPlaceOption.SHOULD_PLACE_NEXT, Pair.of(GUIPlacement.of(false), null));
        } else {
            placementOverride.put(GUIPlaceOption.SHOULD_PLACE_NEXT, Pair.of(GUIPlacement.of(
                                    placementOptions.containsKey(GUIPlaceOption.SHOULD_PLACE_NEXT) ? placementOptions.get(GUIPlaceOption.SHOULD_PLACE_NEXT).slot() : getSize() - 1,
                                    true),
                            new GUIItem(Material.ARROW, "Next")
                                    .addOnClick(() -> next((Player) getInventory().getViewers().getFirst()), ClickType.UNKNOWN)
                                    .addOnClick(() -> next((Player) getInventory().getViewers().getFirst(), 7), ClickType.SHIFT_LEFT)
                    )
            );
        }

        this.fillEmptySlots();
        this.placeOptions();
        this.getInventory() = Bukkit.createInventory((InventoryHolder)null, this.size, this.title);
        this.items.forEach((key, value) -> {
            if (value != null) {
                this.inventory.setItem(key, value.getItem());
            }

        });
        player.openInventory(this.inventory);
    }
}
