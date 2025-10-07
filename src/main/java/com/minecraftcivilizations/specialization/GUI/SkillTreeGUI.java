package com.minecraftcivilizations.specialization.GUI;

import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.Skill;
import com.minecraftcivilizations.specialization.Skill.SkillLevel;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.Specialization;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import lombok.Getter;
import lombok.Setter;
import minecraftcivilizations.com.minecraftCivilizationsCore.GUI.GUI.GUI;
import minecraftcivilizations.com.minecraftCivilizationsCore.GUI.GUIItem.GUIItem;
import minecraftcivilizations.com.minecraftCivilizationsCore.GUI.GUIPlaceOption;
import minecraftcivilizations.com.minecraftCivilizationsCore.GUI.GUIPlacement;
import minecraftcivilizations.com.minecraftCivilizationsCore.Lore.Lore;
import minecraftcivilizations.com.minecraftCivilizationsCore.MinecraftCivilizationsCore;
import minecraftcivilizations.com.minecraftCivilizationsCore.Options.Pair;
import minecraftcivilizations.com.minecraftCivilizationsCore.Util.ItemUtils;
import minecraftcivilizations.com.minecraftCivilizationsCore.Util.LoreUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class SkillTreeGUI extends GUI {
    @Getter
    private final SkillType skillType;
    @Getter
    @Setter
    private int scrollIndex = 0;
    @Getter
    private final Map<Integer, GUIItem> visibleItems = new HashMap<>(0);

    public SkillTreeGUI(SkillType skillType, Player player) {
        super(Component.text("Your SkillTree"), 54, Map.of(GUIPlaceOption.SHOULD_PLACE_EXIT, GUIPlacement.of(true),
                GUIPlaceOption.SHOULD_PLACE_NEXT, GUIPlacement.of(53, true),
                GUIPlaceOption.SHOULD_PLACE_BACK, GUIPlacement.of(8, true),
                GUIPlaceOption.SHOULD_PLACE_SEARCH, GUIPlacement.of(false),
                GUIPlaceOption.SHOULD_PLACE_BACK_TO_DIFFERENT_MENU, GUIPlacement.of(54, true)));
        this.skillType = skillType;
        placeItems(player);
        Optional<Integer> largestKeyOptional = visibleItems.keySet().stream()
                .max(Integer::compareTo);

        largestKeyOptional.ifPresent(largestIndex -> {
            scrollIndex = (largestIndex - 5 * 7) / (getSize() / 9);
        });
        overridePlacementOptions();
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

        placementOverride.put(GUIPlaceOption.SHOULD_PLACE_EXIT, Pair.of(GUIPlacement.of(
                                getSize() - 6,
                                true),
                        new GUIItem(Material.BARRIER, "Exit")
                                .addOnClick(this::closeGUI)
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

    public void placeItems(Player player) {
        visibleItems.put(3, new GUIItem(Material.BEDROCK, "Level \"7\""));
        visibleItems.put(10, createRailItem(CoreUtil.getPlayer(player).getSkillLevelEnum(skillType), SkillLevel.getSkillLevelFromInt(5)));

        for (int i = 1; i < 5; i++) {
            int j = (int) Math.round(Skill.mapValue(i, 1, 5, 5, 1));
            visibleItems.put(14 * i + 1, createSkillItem(player, 0, SkillLevel.getSkillLevelFromInt(j - 1)));
            visibleItems.put(14 * i + 2, createSkillItem(player, 1, SkillLevel.getSkillLevelFromInt(j - 1)));
            visibleItems.put(14 * i + 4, createSkillItem(player, 2, SkillLevel.getSkillLevelFromInt(j - 1)));
            visibleItems.put(14 * i + 5, createSkillItem(player, 3, SkillLevel.getSkillLevelFromInt(j - 1)));
            visibleItems.put(14 * i + 10, createRailItem(CoreUtil.getPlayer(player).getSkillLevelEnum(skillType), SkillLevel.getSkillLevelFromInt(j - 1)));
        }

        if (skillType == SkillType.BLACKSMITH) {
            visibleItems.put(17, new GUIItem(Material.NETHERITE_HELMET, SkillLevel.getDisplayName(SkillLevel.getSkillLevelFromInt(4))));
            visibleItems.put(31, new GUIItem(Material.DIAMOND_HELMET, SkillLevel.getDisplayName(SkillLevel.getSkillLevelFromInt(3))));
            visibleItems.put(45, new GUIItem(Material.IRON_HELMET, SkillLevel.getDisplayName(SkillLevel.getSkillLevelFromInt(2))));
            visibleItems.put(59, new GUIItem(Material.GOLDEN_HELMET, SkillLevel.getDisplayName(SkillLevel.getSkillLevelFromInt(1))));
            visibleItems.put(73, new GUIItem(Material.LEATHER_HELMET, SkillLevel.getDisplayName(SkillLevel.getSkillLevelFromInt(0))));
        } else if (skillType == SkillType.FARMER) {
            visibleItems.put(17, new GUIItem(Material.NETHERITE_HOE, SkillLevel.getDisplayName(SkillLevel.getSkillLevelFromInt(4))));
            visibleItems.put(31, new GUIItem(Material.DIAMOND_HOE, SkillLevel.getDisplayName(SkillLevel.getSkillLevelFromInt(3))));
            visibleItems.put(45, new GUIItem(Material.IRON_HOE, SkillLevel.getDisplayName(SkillLevel.getSkillLevelFromInt(2))));
            visibleItems.put(59, new GUIItem(Material.GOLDEN_HOE, SkillLevel.getDisplayName(SkillLevel.getSkillLevelFromInt(1))));
            visibleItems.put(73, new GUIItem(Material.STONE_HOE, SkillLevel.getDisplayName(SkillLevel.getSkillLevelFromInt(0))));
        } else if (skillType == SkillType.BUILDER) {
            visibleItems.put(17, new GUIItem(Material.NETHERITE_AXE, SkillLevel.getDisplayName(SkillLevel.getSkillLevelFromInt(4))));
            visibleItems.put(31, new GUIItem(Material.DIAMOND_AXE, SkillLevel.getDisplayName(SkillLevel.getSkillLevelFromInt(3))));
            visibleItems.put(45, new GUIItem(Material.IRON_AXE, SkillLevel.getDisplayName(SkillLevel.getSkillLevelFromInt(2))));
            visibleItems.put(59, new GUIItem(Material.GOLDEN_AXE, SkillLevel.getDisplayName(SkillLevel.getSkillLevelFromInt(1))));
            visibleItems.put(73, new GUIItem(Material.STONE_AXE, SkillLevel.getDisplayName(SkillLevel.getSkillLevelFromInt(0))));
        } else if (skillType == SkillType.HEALER) {
            visibleItems.put(17, new GUIItem(Material.PAPER, SkillLevel.getDisplayName(SkillLevel.getSkillLevelFromInt(4))));
            visibleItems.put(31, new GUIItem(Material.ENCHANTED_GOLDEN_APPLE, SkillLevel.getDisplayName(SkillLevel.getSkillLevelFromInt(3))));
            visibleItems.put(45, new GUIItem(Material.GOLDEN_APPLE, SkillLevel.getDisplayName(SkillLevel.getSkillLevelFromInt(2))));
            visibleItems.put(59, new GUIItem(Material.GOLDEN_CARROT, SkillLevel.getDisplayName(SkillLevel.getSkillLevelFromInt(1))));
            visibleItems.put(73, new GUIItem(Material.DRIED_KELP, SkillLevel.getDisplayName(SkillLevel.getSkillLevelFromInt(0))));
        } else if (skillType == SkillType.MINER) {
            visibleItems.put(17, new GUIItem(Material.NETHERITE_PICKAXE, SkillLevel.getDisplayName(SkillLevel.getSkillLevelFromInt(4))));
            visibleItems.put(31, new GUIItem(Material.DIAMOND_PICKAXE, SkillLevel.getDisplayName(SkillLevel.getSkillLevelFromInt(3))));
            visibleItems.put(45, new GUIItem(Material.IRON_PICKAXE, SkillLevel.getDisplayName(SkillLevel.getSkillLevelFromInt(2))));
            visibleItems.put(59, new GUIItem(Material.GOLDEN_PICKAXE, SkillLevel.getDisplayName(SkillLevel.getSkillLevelFromInt(1))));
            visibleItems.put(73, new GUIItem(Material.STONE_PICKAXE, SkillLevel.getDisplayName(SkillLevel.getSkillLevelFromInt(0))));
        } else if (skillType == SkillType.LIBRARIAN) {
            visibleItems.put(17, new GUIItem(Material.KNOWLEDGE_BOOK, SkillLevel.getDisplayName(SkillLevel.getSkillLevelFromInt(4))));
            visibleItems.put(31, new GUIItem(Material.ENCHANTED_BOOK, SkillLevel.getDisplayName(SkillLevel.getSkillLevelFromInt(3))));
            visibleItems.put(45, new GUIItem(Material.WRITABLE_BOOK, SkillLevel.getDisplayName(SkillLevel.getSkillLevelFromInt(2))));
            visibleItems.put(59, new GUIItem(Material.WRITTEN_BOOK, SkillLevel.getDisplayName(SkillLevel.getSkillLevelFromInt(1))));
            visibleItems.put(73, new GUIItem(Material.BOOK, SkillLevel.getDisplayName(SkillLevel.getSkillLevelFromInt(0))));
        }


    }

    public GUIItem createRailItem(SkillLevel skillLevel, SkillLevel neededSkillLevel) {
        return new GUIItem(skillLevel.getLevel() >= neededSkillLevel.getLevel() ? Material.RAIL : Material.POWERED_RAIL, SkillLevel.getDisplayName(neededSkillLevel) + (skillLevel.getLevel() >= neededSkillLevel.getLevel() ? " reached" : " not reached"));
    }

    public GUIItem createLockedItem(SkillLevel skillLevel, int perkIndex, Player player) {
        Lore lore = new Lore()
                .addLoreLines(LoreUtils.createDescriptionLoreLine(SpecializationConfig.getPerkConfig().get(skillType + "_" + skillLevel + "_DESCRIPTION_" + perkIndex, String.class)))
                .addEmptyLine()
                .addLoreLines(LoreUtils.createDescriptionLoreLine("This perk is technically locked, you can unlock it by click it and confirming it, but if you do that the other perks in this class of this tier will be locked"))
                .addEmptyLine()
                .addLoreLine("(Maximum perks per level is 1)", NamedTextColor.GRAY);
        ItemStack itemStack = ItemStack.of(Material.BARRIER);
        itemStack.lore(lore.build());
        itemStack.editMeta(meta -> {
            meta.displayName(LoreUtils.createLoreLine(SpecializationConfig.getPerkConfig().get(skillType + "_" + skillLevel + "_DISPLAY_NAME_" + perkIndex, String.class)));
        });
        return new GUIItem(itemStack).addOnClick(() -> {
            new ConfirmationGUI(() -> {
                CoreUtil.getPlayer(player).getSkillTree().getBranches().get(skillType).getPerks().get(skillLevel).get(0).setUnlocked(false);
                CoreUtil.getPlayer(player).getSkillTree().getBranches().get(skillType).getPerks().get(skillLevel).get(1).setUnlocked(false);
                CoreUtil.getPlayer(player).getSkillTree().getBranches().get(skillType).getPerks().get(skillLevel).get(2).setUnlocked(false);
                CoreUtil.getPlayer(player).getSkillTree().getBranches().get(skillType).getPerks().get(skillLevel).get(3).setUnlocked(false);

                CoreUtil.getPlayer(player).getSkillTree().getBranches().get(skillType).getPerks().get(skillLevel).get(perkIndex).setUnlocked(true);

                this.open(player);
            }, () -> {
                this.open(player);
            }).open(player);
        });
    }

    public GUIItem createUnavailableItem(SkillLevel skillLevel, int perkIndex) {
        Lore lore = new Lore()
                .addLoreLines(LoreUtils.createDescriptionLoreLine("This perk is currently unavailable for you!"))
                .addEmptyLine()
                .addLoreLine("(Reach " + SkillLevel.getDisplayName(skillLevel) + ")", NamedTextColor.GRAY);
        ItemStack itemStack = ItemStack.of(Material.RED_STAINED_GLASS_PANE);
        itemStack.lore(lore.build());
        itemStack.editMeta(meta -> {
            meta.displayName(LoreUtils.createLoreLine(SpecializationConfig.getPerkConfig().get(skillType + "_" + skillLevel + "_DISPLAY_NAME_" + perkIndex, String.class)));
        });
        return new GUIItem(itemStack);
    }

    public GUIItem createAvailableItem(SkillLevel skillLevel, int perkIndex, Player player) {
        Lore lore = new Lore()
                .addLoreLines(LoreUtils.createDescriptionLoreLine(SpecializationConfig.getPerkConfig().get(skillType + "_" + skillLevel + "_DESCRIPTION_" + perkIndex, String.class)))
                .addEmptyLine()
                .addLoreLine("You can unlock this perk!", NamedTextColor.GRAY);
        ItemStack itemStack = ItemStack.of(Material.ORANGE_STAINED_GLASS_PANE);
        itemStack.lore(lore.build());
        itemStack.editMeta(meta -> {
            meta.displayName(LoreUtils.createLoreLine(SpecializationConfig.getPerkConfig().get(skillType + "_" + skillLevel + "_DISPLAY_NAME_" + perkIndex, String.class)));
        });
        return new GUIItem(itemStack)
                .addOnClick(() -> {
                    if (CoreUtil.getPlayer(player).getSkillTree().getBranches().get(skillType).getAvailablePoints() > 0) {
                        CoreUtil.getPlayer(player).getSkillTree().getBranches().get(skillType).getPerks().get(skillLevel).get(perkIndex).setUnlocked(true);
                        CoreUtil.getPlayer(player).getSkillTree().getBranches().get(skillType).removePoint();
                        open(player);
                    }
                })
                .addOnClick(() -> {
                    if (CoreUtil.getPlayer(player).getSkillTree().getBranches().get(skillType).getPerks().get(skillLevel).get(perkIndex).isUnlocked()) {
                        new ConfirmationGUI(() -> {
                            CoreUtil.getPlayer(player).getSkillTree().getBranches().get(skillType).getPerks().get(skillLevel).get(perkIndex).setUnlocked(false);
                            CoreUtil.getPlayer(player).getSkillTree().getBranches().get(skillType).addPoint();
                            open(player);
                        }, () -> {
                            open(player);
                        }).open(player);

                    }
                }, ClickType.RIGHT);
    }

    public GUIItem createUnlockedItem(SkillLevel skillLevel, int perkIndex, Player player) {
        Lore lore = new Lore()
                .addLoreLines(LoreUtils.createDescriptionLoreLine(SpecializationConfig.getPerkConfig().get(skillType + "_" + skillLevel + "_DESCRIPTION_" + perkIndex, String.class)))
                .addEmptyLine()
                .addLoreLine("You've unlocked this perk!", NamedTextColor.GRAY);
        ItemStack itemStack = ItemStack.of(Material.GREEN_STAINED_GLASS);
        itemStack.lore(lore.build());
        itemStack.editMeta(meta -> {
            meta.displayName(LoreUtils.createLoreLine(SpecializationConfig.getPerkConfig().get(skillType + "_" + skillLevel + "_DISPLAY_NAME_" + perkIndex, String.class)));
        });
        return new GUIItem(itemStack)
                .addOnClick(() -> {
                    if (CoreUtil.getPlayer(player).getSkillTree().getBranches().get(skillType).getAvailablePoints() > 0) {
                        CoreUtil.getPlayer(player).getSkillTree().getBranches().get(skillType).getPerks().get(skillLevel).get(perkIndex).setUnlocked(true);
                        CoreUtil.getPlayer(player).getSkillTree().getBranches().get(skillType).removePoint();
                        open(player);
                    }
                })
                .addOnClick(() -> {
                    if (CoreUtil.getPlayer(player).getSkillTree().getBranches().get(skillType).getPerks().get(skillLevel).get(perkIndex).isUnlocked()) {
                        new ConfirmationGUI(() -> {
                            CoreUtil.getPlayer(player).getSkillTree().getBranches().get(skillType).getPerks().get(skillLevel).get(perkIndex).setUnlocked(false);
                            CoreUtil.getPlayer(player).getSkillTree().getBranches().get(skillType).addPoint();
                            open(player);
                        }, () -> {
                            open(player);
                        }).open(player);

                    }
                }, ClickType.RIGHT);
    }

    public GUIItem createSkillItem(Player player, int perkIndex, SkillLevel level) {
        CustomPlayer customPlayer = CoreUtil.getPlayer(player);
        SkillLevel skillLevel = customPlayer.getSkillLevelEnum(skillType);
        if (SpecializationConfig.getPerkConfig().get(skillType + "_" + level + "_" + "DISPLAY_NAME_" + perkIndex, String.class) == null) {
            if (this.getOptions().isShouldSetBackground()) {
                return ItemUtils.makeGUIItemOfType(customPlayer.getPlayerOptions().getDefaultBackgroundMaterial(), "");
            } else {
                return new GUIItem(Material.AIR, "");
            }
        }
        if (skillLevel.getLevel() < level.getLevel()) {
            return createUnavailableItem(level, perkIndex);
        }
        boolean shouldBeLocked = false;

        for (int i = 0; i < 4; i++) {
            if (shouldBeLocked) break;
            if (i == perkIndex) continue;

            if (customPlayer.getSkillTree().getBranches().get(skillType).getPerks().get(level).get(i).isUnlocked())
                shouldBeLocked = true;
        }

        if (shouldBeLocked) {
            return createLockedItem(level, perkIndex, player);
        } else {
            if (customPlayer.getSkillTree().getBranches().get(skillType).getPerks().get(level).get(perkIndex).isUnlocked()) {
                return createUnlockedItem(level, perkIndex, player);
            } else {
                return createAvailableItem(level, perkIndex, player);
            }
        }
    }

    @Override
    public void open(Player player) {
        placeItems(player);
        Optional<Integer> largestKeyOptional = visibleItems.keySet().stream()
                .max(Integer::compareTo);

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
        GUIItem guiItem = new GUIItem(Material.LAPIS_LAZULI, "You have: " +
                CoreUtil.getPlayer(player).getSkillTree().getBranches().get(skillType).getAvailablePoints() + " points");
        guiItem.getItem().setAmount(CoreUtil.getPlayer(player).getSkillTree().getBranches().get(skillType).getAvailablePoints());
        this.getItems().put(26, guiItem);
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
        Optional<Integer> largestKeyOptional = visibleItems.keySet().stream()
                .max(Integer::compareTo);

        if (largestKeyOptional.isPresent() && scrollIndex + amount > (largestKeyOptional.get() - 5 * 7) / (getSize() / 9)) {
            next(player, amount - 1);
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
