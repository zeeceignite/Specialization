package com.minecraftcivilizations.specialization.GUI;

import com.minecraftcivilizations.specialization.Specialization;
import lombok.Getter;
import lombok.Setter;
import minecraftcivilizations.com.minecraftCivilizationsCore.GUI.GUI.GUI;
import minecraftcivilizations.com.minecraftCivilizationsCore.GUI.GUIItem.GUIItem;
import minecraftcivilizations.com.minecraftCivilizationsCore.GUI.GUIPlaceOption;
import minecraftcivilizations.com.minecraftCivilizationsCore.GUI.GUIPlacement;
import minecraftcivilizations.com.minecraftCivilizationsCore.Options.Pair;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class SkillTreeGUI extends GUI {
    @Getter
    @Setter
    private int scrollIndex = 0;
    @Getter
    private final Map<Integer, GUIItem> visibleItems = new HashMap<>(0);

    public SkillTreeGUI() {
        super(Component.text("Your SkillTree"), 54, Map.of(GUIPlaceOption.SHOULD_PLACE_EXIT, GUIPlacement.of(true),
                GUIPlaceOption.SHOULD_PLACE_NEXT, GUIPlacement.of(53, true),
                GUIPlaceOption.SHOULD_PLACE_BACK, GUIPlacement.of(8, true),
                GUIPlaceOption.SHOULD_PLACE_SEARCH, GUIPlacement.of(false),
                GUIPlaceOption.SHOULD_PLACE_BACK_TO_DIFFERENT_MENU, GUIPlacement.of(true)));
        overridePlacementOptions();
        placeItems();
        Optional<Integer> largestKeyOptional = visibleItems.keySet().stream()
                .max(Integer::compareTo);

        largestKeyOptional.ifPresent(largestIndex -> {
            scrollIndex = (largestIndex - 5 * 7) / (getSize() / 9);
        });
    }

    private void overridePlacementOptions() {
        placementOverride.put(GUIPlaceOption.SHOULD_PLACE_BACK, Pair.of(GUIPlacement.of(
                                placementOptions.containsKey(GUIPlaceOption.SHOULD_PLACE_EXIT) ? placementOptions.get(GUIPlaceOption.SHOULD_PLACE_BACK).slot() : getSize() - 1,
                                true),
                        new GUIItem(Material.ARROW, "Down")
                                .addOnClick(() -> back((Player) getInventory().getViewers().getFirst()), ClickType.UNKNOWN)
                                .addOnClick(() -> back((Player) getInventory().getViewers().getFirst(), 7), ClickType.SHIFT_LEFT)
                )
        );

        placementOverride.put(GUIPlaceOption.SHOULD_PLACE_NEXT, Pair.of(GUIPlacement.of(
                                placementOptions.containsKey(GUIPlaceOption.SHOULD_PLACE_NEXT) ? placementOptions.get(GUIPlaceOption.SHOULD_PLACE_NEXT).slot() : 8,
                                true),
                        new GUIItem(Material.ARROW, "Up")
                                .addOnClick(() -> next((Player) getInventory().getViewers().getFirst()), ClickType.UNKNOWN)
                                .addOnClick(() -> next((Player) getInventory().getViewers().getFirst(), 7), ClickType.SHIFT_LEFT)
                )
        );
    }

    public void placeItems() {
        // ROW 1
        visibleItems.put(3, new GUIItem(Material.NETHERITE_HOE, "Level \"7\""));
        // ROW 2
        visibleItems.put(10, new GUIItem(Material.RAIL, "Not Unlocked"));
        // ROW 3
        visibleItems.put(15, new GUIItem(Material.GREEN_STAINED_GLASS_PANE, "Level 6"));
        visibleItems.put(16, new GUIItem(Material.GREEN_STAINED_GLASS_PANE, "Level 6"));
        visibleItems.put(17, new GUIItem(Material.DIAMOND_HOE, "Level 6"));
        visibleItems.put(18, new GUIItem(Material.GREEN_STAINED_GLASS_PANE, "Level 6"));
        visibleItems.put(19, new GUIItem(Material.GREEN_STAINED_GLASS_PANE, "Level 6"));
        // ROW 4
        visibleItems.put(24, new GUIItem(Material.RAIL, "Not Unlocked"));
        // ROW 5
        visibleItems.put(29, new GUIItem(Material.GREEN_STAINED_GLASS_PANE, "Level 5"));
        visibleItems.put(30, new GUIItem(Material.GREEN_STAINED_GLASS_PANE, "Level 5"));
        visibleItems.put(31, new GUIItem(Material.IRON_HOE, "Level 5"));
        visibleItems.put(32, new GUIItem(Material.GREEN_STAINED_GLASS_PANE, "Level 5"));
        visibleItems.put(33, new GUIItem(Material.GREEN_STAINED_GLASS_PANE, "Level 5"));
        // ROW 6
        visibleItems.put(38, new GUIItem(Material.RAIL, "Not Unlocked"));
        // ROW 7
        visibleItems.put(43, new GUIItem(Material.GREEN_STAINED_GLASS_PANE, "Level 4"));
        visibleItems.put(44, new GUIItem(Material.GREEN_STAINED_GLASS_PANE, "Level 4"));
        visibleItems.put(45, new GUIItem(Material.GOLDEN_HOE, "Level 4"));
        visibleItems.put(46, new GUIItem(Material.GREEN_STAINED_GLASS_PANE, "Level 4"));
        visibleItems.put(47, new GUIItem(Material.GREEN_STAINED_GLASS_PANE, "Level 4"));
        // ROW 8
        visibleItems.put(52, new GUIItem(Material.RAIL, "Not Unlocked"));
        // ROW 9
        visibleItems.put(57, new GUIItem(Material.GREEN_STAINED_GLASS_PANE, "Level 3"));
        visibleItems.put(58, new GUIItem(Material.GREEN_STAINED_GLASS_PANE, "Level 3"));
        visibleItems.put(59, new GUIItem(Material.STONE_HOE, "Level 3"));
        visibleItems.put(60, new GUIItem(Material.GREEN_STAINED_GLASS_PANE, "Level 3"));
        visibleItems.put(61, new GUIItem(Material.GREEN_STAINED_GLASS_PANE, "Level 3"));
        // ROW 10
        visibleItems.put(66, new GUIItem(Material.RAIL, "Not Unlocked"));
        // ROW 11
        visibleItems.put(71, new GUIItem(Material.GREEN_STAINED_GLASS_PANE, "Level 2"));
        visibleItems.put(72, new GUIItem(Material.GREEN_STAINED_GLASS_PANE, "Level 2"));
        visibleItems.put(73, new GUIItem(Material.WOODEN_HOE, "Level 2"));
        visibleItems.put(74, new GUIItem(Material.GREEN_STAINED_GLASS_PANE, "Level 2"));
        visibleItems.put(75, new GUIItem(Material.GREEN_STAINED_GLASS_PANE, "Level 2"));


    }

    @Override
    public void open(Player player) {
        int columns = getSize() / 9 - 1;
        int offset = scrollIndex * 7;
        for (int column = 0; column < columns; column++) {
            for (int row = 0; row < 7; row++) {
                if (visibleItems.containsKey(offset + ((column + 1) * 7 - row))) {
                    this.getItems().put(((column + 1) * 7 - row) + column * 2, visibleItems.get(offset + ((column + 1) * 7 - row)));
                } else {
                    this.getItems().remove(((column + 1) * 7 - row) + column * 2);
                }
            }
        }

        if (scrollIndex == 0 && getParentGUI() == null) {
            this.getItems().remove(placementOverride.get(GUIPlaceOption.SHOULD_PLACE_BACK).firstValue().slot());
            placementOverride.put(GUIPlaceOption.SHOULD_PLACE_BACK, Pair.of(GUIPlacement.of(false), null));
        } else {
            placementOverride.put(GUIPlaceOption.SHOULD_PLACE_BACK, Pair.of(GUIPlacement.of(
                                    placementOptions.containsKey(GUIPlaceOption.SHOULD_PLACE_EXIT) ? placementOptions.get(GUIPlaceOption.SHOULD_PLACE_BACK).slot() : getSize() - 9,
                                    true),
                            new GUIItem(Material.ARROW, "Up")
                                    .addOnClick(() -> back((Player) getInventory().getViewers().getFirst()), ClickType.UNKNOWN)
                                    .addOnClick(() -> back((Player) getInventory().getViewers().getFirst(), 7), ClickType.SHIFT_LEFT)
                    )
            );
        }

        Optional<Integer> largestKeyOptional = visibleItems.keySet().stream()
                .max(Integer::compareTo);

        if (largestKeyOptional.isPresent() && largestKeyOptional.get() < scrollIndex * 7 + (7 * columns)) {
            this.getItems().remove(placementOverride.get(GUIPlaceOption.SHOULD_PLACE_NEXT).firstValue().slot());
            placementOverride.put(GUIPlaceOption.SHOULD_PLACE_NEXT, Pair.of(GUIPlacement.of(false), null));
        } else {
            placementOverride.put(GUIPlaceOption.SHOULD_PLACE_NEXT, Pair.of(GUIPlacement.of(
                                    placementOptions.containsKey(GUIPlaceOption.SHOULD_PLACE_NEXT) ? placementOptions.get(GUIPlaceOption.SHOULD_PLACE_NEXT).slot() : getSize() - 1,
                                    true),
                            new GUIItem(Material.ARROW, "Down")
                                    .addOnClick(() -> next((Player) getInventory().getViewers().getFirst()), ClickType.UNKNOWN)
                                    .addOnClick(() -> next((Player) getInventory().getViewers().getFirst(), 7), ClickType.SHIFT_LEFT)
                    )
            );
        }

        this.getItems().put(7, new GUIItem(Material.TWISTING_VINES, ""));
        this.getItems().put(16, new GUIItem(Material.TWISTING_VINES, ""));
        this.getItems().put(25, new GUIItem(Material.TWISTING_VINES, ""));
        this.getItems().put(34, new GUIItem(Material.TWISTING_VINES, ""));
        this.getItems().put(43, new GUIItem(Material.TWISTING_VINES, ""));
        this.getItems().put(52, new GUIItem(Material.TWISTING_VINES, ""));

        super.open(player);
    }

    @Override
    public void next(Player player) {
        scrollIndex++;
        open(player);
    }

    public void next(Player player, int amount) {
        int columns = getSize() / 9 - 1;


        Optional<Integer> largestKeyOptional = visibleItems.keySet().stream()
                .max(Integer::compareTo);

        if (largestKeyOptional.isPresent() && scrollIndex + amount > (largestKeyOptional.get() - 5 * 7) / (getSize() / 9)) {
            next(player, amount-1);
            return;
        }

        scrollIndex += amount;
        open(player);
    }

    @Override
    public void back(Player player) {
        if (scrollIndex > 0) {
            scrollIndex--;
            open(player);
            return;
        }
        super.back(player);
    }

    public void back(Player player, int amount) {
        if (scrollIndex - amount < 0) scrollIndex = 0;
        else scrollIndex -= amount;
        open(player);
    }
}
