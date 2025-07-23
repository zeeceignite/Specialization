package com.minecraftcivilizations.specialization.GUI;

import com.minecraftcivilizations.specialization.Config.Config;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.Skill;
import com.minecraftcivilizations.specialization.Skill.SkillLevel;
import com.minecraftcivilizations.specialization.Specialization;
import minecraftcivilizations.com.minecraftCivilizationsCore.GUI.GUI;
import minecraftcivilizations.com.minecraftCivilizationsCore.GUI.GUIItem;
import minecraftcivilizations.com.minecraftCivilizationsCore.GUI.ListGUI;
import minecraftcivilizations.com.minecraftCivilizationsCore.MinecraftCivilizationsCore;
import minecraftcivilizations.com.minecraftCivilizationsCore.item.ItemUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainMenuGUI extends GUI {

    public MainMenuGUI(String title) {
        super(Component.text(title), 54);
        this.getItems().put(0, new GUIItem(new ItemStack(Material.COMPASS), new Runnable() {
            @Override
            public void run() {
                List<ItemStack> items = new ArrayList<>();
                for (Material material : Material.values()) {
                    if (material.isItem() && material != Material.AIR) {
                        items.add(new ItemStack(material));
                    }
                }

                new ListGUI(Component.text("Classes"), new ArrayList<>(items)).setParentGUI(MainMenuGUI.this).open((Player) getInventory().getViewers().getFirst());
            }
        }));
    }

    @Override
    public void open(Player player) {
        if (MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().getCustomPlayer(player.getUniqueId()) != null) {
            CustomPlayer customPlayer = (CustomPlayer) MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().getCustomPlayer(player.getUniqueId());
            this.getItems().put(4, ItemUtils.makeGUIItemOfType(Material.EMERALD, customPlayer.getInGameName()));
            int i = 37;
            for (Skill skill : customPlayer.getSkills()) {
                int temp = i;
                for (int score = 0; score < 3; score++) {
                    double diff = customPlayer.getGUIDistributionOfTotalSkills(skill.getSkillType()) - score;



                    if (diff >= 1) {
                        this.getItems().put(temp-=9, makeGlassDistributionPane(Skill.getDisplayName(skill.getSkillType()), Material.GREEN_STAINED_GLASS_PANE, customPlayer.getPercentOfTotal(skill.getSkillType())));
                    } else if (diff >= 0.75) {
                        this.getItems().put(temp-=9, makeGlassDistributionPane(Skill.getDisplayName(skill.getSkillType()), Material.LIME_STAINED_GLASS_PANE, customPlayer.getPercentOfTotal(skill.getSkillType())));
                    } else if (diff >= 0.5) {
                        this.getItems().put(temp-=9, makeGlassDistributionPane(Skill.getDisplayName(skill.getSkillType()), Material.YELLOW_STAINED_GLASS_PANE, customPlayer.getPercentOfTotal(skill.getSkillType())));
                    } else if (diff >= 0.25) {
                        this.getItems().put(temp-=9, makeGlassDistributionPane(Skill.getDisplayName(skill.getSkillType()), Material.ORANGE_STAINED_GLASS_PANE, customPlayer.getPercentOfTotal(skill.getSkillType())));
                    } else {
                        this.getItems().put(temp-=9, makeGlassDistributionPane(Skill.getDisplayName(skill.getSkillType()), Material.RED_STAINED_GLASS_PANE, customPlayer.getPercentOfTotal(skill.getSkillType())));
                    }

                }

                int currentSkillLevel = Skill.getLevelFromXP(skill.getXp());

                if (currentSkillLevel < SkillLevel.values().length) {

                    double currentXp = Math.round(skill.getXp() * 100) / 100D;
                    double xpToNextLevel = Math.round((Skill.getXPNeededForLevel(currentSkillLevel + 1) - skill.getXp()) * 100) / 100D ;
                    double percentOfNextLevel = Math.round(Config.getSkillRequirementsConfig().getDouble(skill.getSkillType().name() + "_" + SkillLevel.getSkillLevelFromInt(currentSkillLevel + 1).name() + "_REQUIREMENT") * 100) / 100D;

                    Specialization.logger.info(String.valueOf(customPlayer.getPercentOfTotal(skill.getSkillType())));

                    ItemStack itemStack = ItemStack.of(skill.getSkillType().getSkillWorkstation());
                    ItemMeta itemMeta = itemStack.getItemMeta();
                    itemMeta.addItemFlags(ItemFlag.values());
                    itemMeta.displayName(Component.text(Skill.getDisplayName(skill.getSkillType()))
                            .decoration(TextDecoration.ITALIC, false)
                            .color(NamedTextColor.WHITE));
                    itemMeta.lore(new ArrayList<>() {
                        {
                            add(Component.text(Skill.getDisplayName(skill.getSkillLevel()))
                                    .decoration(TextDecoration.ITALIC, false)
                                    .color(NamedTextColor.WHITE)
                                    .append(Component.text("(lvl " + currentSkillLevel + ")"))
                                    .color(NamedTextColor.GRAY)
                                    .decoration(TextDecoration.ITALIC, false));
                            add(Component.text("Current xp: " + (int) Math.round(skill.getXp())).decoration(TextDecoration.ITALIC, false).color(NamedTextColor.WHITE));
                            add(Component.empty());
                            add(Component.text("Requirements for level " + (currentSkillLevel + 1) + ":").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.WHITE));
                            add(Component.text((int) Math.round(Skill.getXPNeededForLevel(currentSkillLevel + 1)) + "xp").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.WHITE));
                            add(Component.text(percentOfNextLevel + "% of your total xp to level up").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.WHITE));
                            add(Component.empty());
                            add(Component.text("You are missing: ").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.GRAY));
                            if (xpToNextLevel > 0) {
                                add(Component.text(xpToNextLevel + "xp to have enough xp").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.GRAY));
                            }
                            if (percentOfNextLevel > customPlayer.getPercentOfTotal(skill.getSkillType())) {
                                add(Component.text(Math.round((percentOfNextLevel - customPlayer.getPercentOfTotal(skill.getSkillType())) * 100) / 100.0 + "% more to level up")
                                        .color(NamedTextColor.WHITE)
                                        .decoration(TextDecoration.ITALIC, false)
                                        .append(Component.text(" (" + (int) Math.floor((customPlayer.getTotalXp() * percentOfNextLevel) - currentXp) + "xp)"))
                                        .color(NamedTextColor.GRAY)
                                        .decoration(TextDecoration.ITALIC, false));
                            }
                            add(Component.empty());
                            add(Component.text("Pretty awesome if you ask me!").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.GRAY));
                        }
                    });
                    itemStack.setItemMeta(itemMeta);
                    this.getItems().put(i++, new GUIItem(itemStack, null));
                }

            }


        }
        super.open(player);
    }

    private GUIItem makeGlassDistributionPane(String name, Material material, double percent) {
        ItemStack itemStack = ItemStack.of(material);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.addItemFlags(ItemFlag.values());
        itemMeta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false).color(NamedTextColor.WHITE));
        itemMeta.lore(new ArrayList<>() {
            {
                add(Component.text("Holds " + Math.round(percent * 100) / 100 + "% of your total xp").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.WHITE));
                add(Component.empty());
                add(Component.text("Awesome!").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.GRAY));
            }
        });
        itemStack.setItemMeta(itemMeta);
        return new GUIItem(itemStack, null);
    }
}
