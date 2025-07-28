package com.minecraftcivilizations.specialization.GUI;

import com.minecraftcivilizations.specialization.Config.Config;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.Skill;
import com.minecraftcivilizations.specialization.Skill.SkillLevel;
import minecraftcivilizations.com.minecraftCivilizationsCore.GUI.GUI;
import minecraftcivilizations.com.minecraftCivilizationsCore.GUI.GUIItem;
import minecraftcivilizations.com.minecraftCivilizationsCore.GUI.ListGUI;
import minecraftcivilizations.com.minecraftCivilizationsCore.MinecraftCivilizationsCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.minecraftcivilizations.specialization.Skill.SkillType.getDisplayName;

public class MainMenuGUI extends GUI {

    public MainMenuGUI(String title) {
        super(Component.text(title), 54);
        this.getItems().put(0, new GUIItem(new ItemStack(Material.COMPASS), () -> {
            List<ItemStack> items = new ArrayList<>();
            for (Material material : Material.values()) {
                if (material.isItem() && material != Material.AIR) {
                    items.add(new ItemStack(material));
                }
            }

            new ListGUI(Component.text("Classes"), new ArrayList<>(items)).setParentGUI(MainMenuGUI.this).open((Player) getInventory().getViewers().getFirst());
        }));
    }

    private Material getPaneMaterial(int tier, int type){
        String color = switch (tier) {
            case 0 -> "WHITE";
            case 1 -> "RED";
            case 2 -> "ORANGE";
            case 3 -> "YELLOW";
            case 4 -> "LIME";
            case 5 -> "PURPLE";
            default -> "BLACK";
        };
        String materialType = switch (type){
            case 0 -> "CARPET";
            case 1 -> "DYE";
            case 2 -> "STAINED_GLASS_PANE";
            default-> "BANNER";
        };
        return Objects.requireNonNull(Material.getMaterial(color + "_" + materialType));
    }

    @Override
    public void open(Player player) {
        if (MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().getCustomPlayer(player.getUniqueId()) != null) {
            CustomPlayer customPlayer = (CustomPlayer) MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().getCustomPlayer(player.getUniqueId());
//            this.getItems().put(4, ItemUtils.makeGUIItemOfType(ItemStack.of(Material.EMERALD), customPlayer.getName()));
            int i = 37;
            for (Skill skill : customPlayer.getSkills()) {
                int temp = i;
                double distribution = customPlayer.getGUIDistributionOfTotalSkills(skill.getSkillType(), player);
                int currentSkillLevel =  Math.min(customPlayer.getSkillLevel(skill.getSkillType()),SkillLevel.values().length-1);
                for (int score = 0; score < 3; score++) {
                    if((distribution * .03 - score) < 0 ) break;
                    int type = Math.min((int) (distribution * .09 - score * 3), 2);
                    this.getItems().put(temp-=9, makeGlassDistributionPane(getDisplayName(skill.getSkillType()), getPaneMaterial(currentSkillLevel, type), distribution));
                }

                double currentXp = Math.round(skill.getXp() * 100) / 100D;
                double percentOfTotalForNextLevel = Math.round(Config.getSkillRequirementsConfig().getDouble(skill.getSkillType().name() + "_" + SkillLevel.getSkillLevelFromInt(currentSkillLevel + 1).name() + "_REQUIREMENT") * 100) / 100D;
                double xpToNextLevel = Math.max(
                        Math.round((Skill.getXPNeededForLevel(currentSkillLevel + 1) - skill.getXp()) * 100) / 100D,
                        Math.round((percentOfTotalForNextLevel / 100 * customPlayer.getTotalXp() - currentXp) / (1.0 - percentOfTotalForNextLevel / 100))
                );

                ItemStack itemStack = ItemStack.of(skill.getSkillType().getSkillWorkstation());
                ItemMeta itemMeta = itemStack.getItemMeta();
                itemMeta.addItemFlags(ItemFlag.values());
                itemMeta.displayName(Component.text(getDisplayName(skill.getSkillType()))
                        .decoration(TextDecoration.ITALIC, false)
                        .color(NamedTextColor.WHITE));
                itemMeta.lore(new ArrayList<>() {
                    {
                        add(Component.text(SkillLevel.getDisplayName(currentSkillLevel))
                                .decoration(TextDecoration.ITALIC, false)
                                .color(NamedTextColor.WHITE)
                                .append(Component.text("(lvl " + currentSkillLevel + ")"))
                                .color(NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false));
                        add(Component.text("Current xp: " + (int) Math.round(skill.getXp())).decoration(TextDecoration.ITALIC, false).color(NamedTextColor.WHITE));
                        add(Component.empty());
                        add(Component.text("You are missing: ").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.GRAY));
                        if (xpToNextLevel > 0) {
                                add(Component.text(xpToNextLevel + "xp to level up").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.GRAY));
                        }
                    }
                });
                itemStack.setItemMeta(itemMeta);
                this.getItems().put(i++, new GUIItem(itemStack, null));

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
                add(Component.text("You have " + percent + "% progress to the next tier in this skill.").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.WHITE));
                add(Component.empty());
                add(Component.text("Awesome!").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.GRAY));
            }
        });
        itemStack.setItemMeta(itemMeta);
        return new GUIItem(itemStack, null);
    }
}
