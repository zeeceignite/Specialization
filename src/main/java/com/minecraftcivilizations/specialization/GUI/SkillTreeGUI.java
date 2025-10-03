package com.minecraftcivilizations.specialization.GUI;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SkillTreeGUI extends GUI {
    @Getter
    @Setter
    private int scrollIndex = 0;
//    TODO: Make this a Map<Integer, GUIItem> and make all of the stuff in between be empty
    @Getter
    private final List<ItemStack> visibleItems = new ArrayList<>(80);

    public SkillTreeGUI() {
        super(Component.text("Your SkillTree"), 54, Map.of(GUIPlaceOption.SHOULD_PLACE_EXIT, GUIPlacement.of(true),
                GUIPlaceOption.SHOULD_PLACE_NEXT, GUIPlacement.of(53, true),
                GUIPlaceOption.SHOULD_PLACE_BACK, GUIPlacement.of(8, true),
                GUIPlaceOption.SHOULD_PLACE_SEARCH, GUIPlacement.of(false),
                GUIPlaceOption.SHOULD_PLACE_BACK_TO_DIFFERENT_MENU, GUIPlacement.of(true)));
        overridePlacementOptions();
    }

    private void overridePlacementOptions() {
        placementOverride.put(GUIPlaceOption.SHOULD_PLACE_BACK, Pair.of(GUIPlacement.of(
                                placementOptions.containsKey(GUIPlaceOption.SHOULD_PLACE_EXIT) ? placementOptions.get(GUIPlaceOption.SHOULD_PLACE_BACK).slot() : getSize() - 1,
                                true),
                        new GUIItem(Material.ARROW, "Next")
                                .addOnClick(() -> back((Player) getInventory().getViewers().getFirst()), ClickType.UNKNOWN)
                                .addOnClick(() -> back((Player) getInventory().getViewers().getFirst(), 7), ClickType.SHIFT_LEFT)
                )
        );

        placementOverride.put(GUIPlaceOption.SHOULD_PLACE_NEXT, Pair.of(GUIPlacement.of(
                                placementOptions.containsKey(GUIPlaceOption.SHOULD_PLACE_NEXT) ? placementOptions.get(GUIPlaceOption.SHOULD_PLACE_NEXT).slot() : 8,
                                true),
                        new GUIItem(Material.ARROW, "Back")
                                .addOnClick(() -> next((Player) getInventory().getViewers().getFirst()), ClickType.UNKNOWN)
                                .addOnClick(() -> next((Player) getInventory().getViewers().getFirst(), 7), ClickType.SHIFT_LEFT)
                )
        );
    }

    public void placeItems() {
        // ROW 1
        visibleItems.set(3, new GUIItem(Material.NETHERITE_HOE, "Level \"7\"").getItem());
        // ROW 2
        visibleItems.set(10, new GUIItem(Material.RAIL, "Not Unlocked").getItem());
        // ROW 3
        visibleItems.set(15, new GUIItem(Material.GREEN_STAINED_GLASS_PANE, "Level 6").getItem());
        visibleItems.set(16, new GUIItem(Material.GREEN_STAINED_GLASS_PANE, "Level 6").getItem());
        visibleItems.set(17, new GUIItem(Material.DIAMOND_HOE, "Level 6").getItem());
        visibleItems.set(18, new GUIItem(Material.GREEN_STAINED_GLASS_PANE, "Level 6").getItem());
        visibleItems.set(19, new GUIItem(Material.GREEN_STAINED_GLASS_PANE, "Level 6").getItem());
        // ROW 4
        visibleItems.set(24, new GUIItem(Material.RAIL, "Not Unlocked").getItem());
        // ROW 5
        visibleItems.set(29, new GUIItem(Material.GREEN_STAINED_GLASS_PANE, "Level 5").getItem());
        visibleItems.set(30, new GUIItem(Material.GREEN_STAINED_GLASS_PANE, "Level 5").getItem());
        visibleItems.set(31, new GUIItem(Material.IRON_HOE, "Level 5").getItem());
        visibleItems.set(32, new GUIItem(Material.GREEN_STAINED_GLASS_PANE, "Level 5").getItem());
        visibleItems.set(33, new GUIItem(Material.GREEN_STAINED_GLASS_PANE, "Level 5").getItem());
        // ROW 6
        visibleItems.set(38, new GUIItem(Material.RAIL, "Not Unlocked").getItem());
        // ROW 7
        visibleItems.set(43, new GUIItem(Material.GREEN_STAINED_GLASS_PANE, "Level 4").getItem());
        visibleItems.set(44, new GUIItem(Material.GREEN_STAINED_GLASS_PANE, "Level 4").getItem());
        visibleItems.set(45, new GUIItem(Material.GOLDEN_HOE, "Level 4").getItem());
        visibleItems.set(46, new GUIItem(Material.GREEN_STAINED_GLASS_PANE, "Level 4").getItem());
        visibleItems.set(47, new GUIItem(Material.GREEN_STAINED_GLASS_PANE, "Level 4").getItem());
        // ROW 8
        visibleItems.set(52, new GUIItem(Material.RAIL, "Not Unlocked").getItem());
        // ROW 9
        visibleItems.set(57, new GUIItem(Material.GREEN_STAINED_GLASS_PANE, "Level 3").getItem());
        visibleItems.set(58, new GUIItem(Material.GREEN_STAINED_GLASS_PANE, "Level 3").getItem());
        visibleItems.set(59, new GUIItem(Material.STONE_HOE, "Level 3").getItem());
        visibleItems.set(60, new GUIItem(Material.GREEN_STAINED_GLASS_PANE, "Level 3").getItem());
        visibleItems.set(61, new GUIItem(Material.GREEN_STAINED_GLASS_PANE, "Level 3").getItem());
        // ROW 10
        visibleItems.set(66, new GUIItem(Material.RAIL, "Not Unlocked").getItem());
        // ROW 11
        visibleItems.set(71, new GUIItem(Material.GREEN_STAINED_GLASS_PANE, "Level 2").getItem());
        visibleItems.set(72, new GUIItem(Material.GREEN_STAINED_GLASS_PANE, "Level 2").getItem());
        visibleItems.set(73, new GUIItem(Material.WOODEN_HOE, "Level 2").getItem());
        visibleItems.set(74, new GUIItem(Material.GREEN_STAINED_GLASS_PANE, "Level 2").getItem());
        visibleItems.set(75, new GUIItem(Material.GREEN_STAINED_GLASS_PANE, "Level 2").getItem());
    }

    @Override
    public void open(Player player) {
        placeItems();
        int itemIndex = 0;
        int offset = scrollIndex * 7;
        for (int placementIndex = 10; placementIndex < getSize() - 9; placementIndex++) {
            if (placementIndex % 7 != 0 && placementIndex % 8 != 0 && placementIndex % 9 != 0 && placementIndex % 9 != 8 && itemIndex < visibleItems.size()) {
                if(visibleItems.get(itemIndex + offset).getType().isItem() && visibleItems.get(itemIndex + offset).getType() != Material.AIR) {
                    this.getItems().put(placementIndex, new GUIItem(visibleItems.get(itemIndex + offset)));
                }
                itemIndex++;
            }
        }


        if (scrollIndex == 0) {
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
        if (visibleItems.size() < (itemIndex + offset)) {
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

        super.open(player);
    }

    @Override
    public void next(Player player) {
        scrollIndex++;
        open(player);
    }

    public void next(Player player, int amount) {
        int rows = getSize() / 9 - 2;
        int offset = (scrollIndex + amount) * rows;
        int itemInCenter = rows * 7;

        if ((itemInCenter + offset) - visibleItems.size() > rows) {
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
