package com.minecraftcivilizations.specialization.Listener;

import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillLevel;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import com.minecraftcivilizations.specialization.Specialization;
import minecraftcivilizations.com.minecraftCivilizationsCore.Item.CustomItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.ChatColor;
import java.util.ArrayList;
import java.util.List;

public class FoodInteractionListener implements Listener {

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || !item.getType().isEdible() || !event.getAction().name().contains("RIGHT_CLICK")) return;
        if (!player.isSneaking()) return;
        CustomPlayer customPlayer = CoreUtil.getPlayer(player.getUniqueId());
        if (customPlayer == null) return;
        if (customPlayer.getSkillLevel(SkillType.HEALER) > SkillLevel.APPRENTICE.getLevel()) {

            if (player.getFoodLevel() < 10) {
                player.sendMessage(ChatColor.RED + "You need more hunger to bless food");
                event.setCancelled(true);
                return;
            }
            
            int healerLevel = customPlayer.getSkillLevel(SkillType.HEALER);
            if (item.getAmount() > 1) {
                ItemStack singleItem = item.clone();
                singleItem.setAmount(1);
                blessFood(singleItem, healerLevel);
                item.setAmount(item.getAmount() - 1);
                if (player.getInventory().firstEmpty() != -1) {
                    player.getInventory().addItem(singleItem);
                } else {
                    player.getWorld().dropItemNaturally(player.getLocation(), singleItem);
                    player.sendMessage(ChatColor.YELLOW + "Your inventory is full! The blessed food was dropped.");
                }
                player.sendMessage(ChatColor.GOLD + "You have blessed one " + getItemName(singleItem));
            } else {
                blessFood(item, healerLevel);
                player.sendMessage(ChatColor.GOLD + "You have blessed this food");
            }
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        CustomPlayer  customPlayer = CoreUtil.getPlayer(player.getUniqueId());
        ItemStack item = event.getItem();
        if (isBlessedFood(item)) {
            int healerLevel = getBlessedFoodLevel(item);
            applyBlessedFoodEffects(player, healerLevel);
        }
        if (customPlayer != null && customPlayer.isDowned()) {
            customPlayer.setDowned(false);
            int healerLevel = getBlessedFoodLevel(item);
            applyBlessedFoodEffects(player, healerLevel);
            player.removePotionEffect(PotionEffectType.WITHER);
        }

    }

    private void blessFood(ItemStack item, int healerLevel) {
        CustomItem customItem = new CustomItem(item.getType(), Component.text("Blessed " + getItemName(item)).color(NamedTextColor.GOLD));
        customItem.addLore(Specialization.getInstance(), List.of(Component.empty(), Component.text("Blessed Food").color(NamedTextColor.YELLOW), Component.text("Healer Level: " + healerLevel).color(NamedTextColor.GRAY), Component.text("Grants regeneration when consumed").color(NamedTextColor.GRAY)));
        item.setItemMeta(customItem.getItem().getItemMeta());
    }

    private void applyBlessedFoodEffects(Player player, int healerLevel) {
        int duration = 400;
        int amplifier = 0;
        if (healerLevel >= SkillLevel.JOURNEYMAN.getLevel()) duration = 600;
        if (healerLevel >= SkillLevel.EXPERT.getLevel()) {
            duration = 800;
            amplifier = 1;
        }
        if (healerLevel >= SkillLevel.MASTER.getLevel()) {
            duration = 1000;
            amplifier = 1;
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 1200, 0));
        }
        if (healerLevel >= SkillLevel.GRANDMASTER.getLevel()) {
            duration = 1200;
            amplifier = 2;
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 1200, 1));
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration, amplifier));
    }

    private int getBlessedFoodLevel(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return 0;
        List<Component> lore = item.getItemMeta().lore();
        if (lore == null) return 0;
        for (Component component : lore) {
            String line = component.toString();
            if (line.contains("Healer Level: ")) {
                String plainText = ((net.kyori.adventure.text.TextComponent) component).content();
                if (plainText.startsWith("Healer Level: ")) {
                    String levelStr = plainText.replace("Healer Level: ", "");
                    try {
                        return Integer.parseInt(levelStr);
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                }
            }
        }
        return 0;
    }

    private boolean isBlessedFood(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return false;
        List<Component> lore = item.getItemMeta().lore();
        if (lore == null) return false;
        for (Component component : lore) {
            if (component instanceof net.kyori.adventure.text.TextComponent) {
                String content = ((net.kyori.adventure.text.TextComponent) component).content();
                if ("Blessed Food".equals(content)) return true;
            }
        }
        return false;
    }

    private String getItemName(ItemStack item) {
        String materialName = item.getType().name();
        String[] words = materialName.toLowerCase().split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (result.length() > 0) result.append(" ");
            result.append(word.substring(0, 1).toUpperCase()).append(word.substring(1));
        }
        return result.toString();
    }
}