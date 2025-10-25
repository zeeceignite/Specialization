package com.minecraftcivilizations.specialization.Listener.Player.Interactions;

import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillLevel;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.Specialization;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import minecraftcivilizations.com.minecraftCivilizationsCore.Item.CustomItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Objects;
import java.util.Random;

public class FoodInteractionListener implements Listener {

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        CustomPlayer customPlayer = CoreUtil.getPlayer(player.getUniqueId());
        if (customPlayer == null) return;
        if (event.getAction().isRightClick()) {
            if (item == null) return;
            if (item.getType().isEdible() && player.isSneaking()) {
                if (customPlayer.getSkillLevel(SkillType.HEALER) > SkillLevel.APPRENTICE.getLevel()) {

                    // Prevent blessing of golden apples and enchanted golden apples
                    if (item.getType() == Material.GOLDEN_APPLE || item.getType() == Material.ENCHANTED_GOLDEN_APPLE) {
                        player.sendMessage(ChatColor.RED + "Golden apples cannot be blessed!");
                        event.setCancelled(true);
                        return;
                    }

                    if (player.getFoodLevel() < 10) {
                        player.sendMessage(ChatColor.RED + "You need more hunger to bless food");
                        event.setCancelled(true);
                        return;
                    }

                    int healerLevel = customPlayer.getSkillLevel(SkillType.HEALER);
                    if (item.getAmount() >= 1) {
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null && meta.hasLore()) {
                            for (String line : meta.getLore()) {
                                if (ChatColor.stripColor(line).toLowerCase().contains("blessed")) {
                                    player.sendMessage(ChatColor.RED + "This food is already blessed!");
                                    event.setCancelled(true);
                                    return;
                                }
                            }
                        }

                        int blessXp = SpecializationConfig.getHealthConfig().get("BLESSED_FOOD_HEALER_XP", Integer.class);
                        int hungerCost = SpecializationConfig.getHealthConfig().get("BLESSED_FOOD_HUNGER_COST", Integer.class);

                        ItemStack singleItem = item.clone();
                        singleItem.setAmount(1);
                        blessFood(singleItem, healerLevel);
                        item.setAmount(item.getAmount() - 1);
                        player.setFoodLevel(player.getFoodLevel() - hungerCost);
                        if (player.getInventory().firstEmpty() != -1) {
                            player.getInventory().addItem(singleItem);
                        } else {
                            player.getWorld().dropItemNaturally(player.getLocation(), singleItem);
                            player.sendMessage(ChatColor.YELLOW + "Your inventory is full! The blessed food was dropped.");
                        }
                        customPlayer.addSkillXp(SkillType.HEALER, blessXp);
                        player.sendMessage(ChatColor.GOLD + "You have blessed one " + getItemName(singleItem));
                    }

                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        CustomPlayer  customPlayer = CoreUtil.getPlayer(player.getUniqueId());
        ItemStack item = event.getItem();

        if (isBlessedFood(item)) {
            int healerLevel = getBlessedFoodLevel(item);
            applyBlessedFoodEffects(player, healerLevel, item.getType());
        }

        if(!customPlayer.eatFood(event.getItem().getType())){
            int reduction = SpecializationConfig.getHungerConfig().get("HUNGER_REDUCTION_ON_NON_UNIQUE_CONSECUTIVE_FOOD", Integer.class);
            player.setSaturation(event.getPlayer().getSaturation() - reduction);
        }

        if (customPlayer != null && customPlayer.isDowned() && isBlessedFood(item)) {
            int healerLevel = getBlessedFoodLevel(item);
            applyBlessedFoodEffects(player, healerLevel, item.getType());
            player.removePotionEffect(PotionEffectType.WITHER);
        }

        if (item.getType().equals(Material.DRIED_KELP)) {
            giveKelpEffects(player);
        }

        if(item.getType().equals(Material.GOLDEN_APPLE)) {
            giveGoldenAppleEffects(player);
        }

    }

    private void giveKelpEffects(Player player){
        if(new Random().nextDouble() < .1) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 20 * 6, 1));
            player.sendRichMessage("<#456e55>You feel a little seasick from eating the kelp.");
        }
    }

    private void giveGoldenAppleEffects(Player player){
        if(new Random().nextDouble() < .2) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 60, 2));
            player.sendRichMessage("<#dbae32>You feel solidified by the golden nature of the apple.");
        }
    }

    private void blessFood(ItemStack item, int healerLevel) {
        String pretty = getItemName(item);
        CustomItem customItem = new CustomItem(item.getType(),
                Component.text("Blessed " + pretty).color(NamedTextColor.GOLD));

        String effectSummary;
        if (healerLevel >= SkillLevel.GRANDMASTER.getLevel()) {
            effectSummary = "Regeneration III 60s, Absorption II 60s";
        } else if (healerLevel >= SkillLevel.MASTER.getLevel()) {
            effectSummary = "Regeneration II 20s, Absorption I 40s";
        } else if (healerLevel >= SkillLevel.EXPERT.getLevel()) {
            effectSummary = "Regeneration II 20s";
        } else { // Journeyman
            effectSummary = "Regeneration I 30s";
        }

        customItem.addLore(Specialization.getInstance(), List.of(
                Component.empty(),
                Component.text("Blessed Food").color(NamedTextColor.YELLOW),
                Component.text("Healer Level: " + healerLevel).color(NamedTextColor.GRAY),
                Component.text(effectSummary).color(NamedTextColor.GRAY)
        ));
        item.setItemMeta(customItem.getItem().getItemMeta());
    }




    private void applyBlessedFoodEffects(Player player, int healerLevel, Material itemType) {
        // Fixed, spec-accurate values (ticks); remove food dependence at L2+
        int regenDurationTicks = 0;
        int regenAmplifier = 0; // amp 0 = Regen I, 1 = Regen II, 2 = Regen III

        // Optional extras
        Integer absorptionDurationTicks = null;
        Integer absorptionAmplifier = null;

        if (healerLevel >= SkillLevel.GRANDMASTER.getLevel()) {
            regenDurationTicks = 20 * 20;
            regenAmplifier = 0;
            absorptionDurationTicks = 20 * 20;
            absorptionAmplifier = 0;
        } else if (healerLevel >= SkillLevel.MASTER.getLevel()) {
            regenDurationTicks = 15 * 20;
            regenAmplifier = 0;
            absorptionDurationTicks = 15 * 20;
            absorptionAmplifier = 0;
        } else if (healerLevel >= SkillLevel.EXPERT.getLevel()) {
            regenDurationTicks = 10 * 20;
            regenAmplifier = 1;
        } else if (healerLevel >= SkillLevel.JOURNEYMAN.getLevel()) {
            regenDurationTicks = 5 * 20;
            regenAmplifier = 0;
            absorptionDurationTicks = 5 * 20;
            absorptionAmplifier = 0;
        } else {
            // Below Journeyman shouldn’t be able to bless; safe no-op
            return;
        }

        boolean saturate = false;
        // Apply effects
        if (regenDurationTicks > 0) {
            saturate = true;
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, regenDurationTicks, regenAmplifier));
        }
        if (absorptionDurationTicks != null && absorptionAmplifier != null) {
            saturate = true;
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, absorptionDurationTicks, absorptionAmplifier));
        }


        // Keep your existing “restore max health if below normal” behavior
        if (SpecializationConfig.getHealthConfig().get("HEALTH_ENABLED", Boolean.class)) {
            double currentMaxHealth = Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getValue();
            double normalMaxHealth = SpecializationConfig.getHealthConfig().get("MAX_HEALTH", Double.class);
            double healthRestoreAmount = SpecializationConfig.getHealthConfig().get("BLESSED_FOOD_HEALTH_RESTORE_AMOUNT", Double.class);

            if (currentMaxHealth < normalMaxHealth) {
                double newMaxHealth = Math.min(normalMaxHealth, currentMaxHealth + healthRestoreAmount);
                Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(newMaxHealth);
                player.sendMessage(ChatColor.GREEN + "You feel your vitality returning! Max health restored to " + (int) newMaxHealth);
            }
        }
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

    private Integer getItemRegen(Material item){
        return switch (item) {
            case CHICKEN,MUTTON,COOKIE,GLOW_BERRIES,MELON_SLICE,POISONOUS_POTATO,
                 COD,SALMON,SPIDER_EYE,SWEET_BERRIES -> 2;
            case CARROT,BEEF,PORKCHOP,RABBIT -> 3;
            case APPLE,CHORUS_FRUIT,GOLDEN_APPLE,ENCHANTED_GOLDEN_APPLE,ROTTEN_FLESH -> 4;
            case BAKED_POTATO,BREAD,COOKED_COD,COOKED_RABBIT -> 5;
            case BEETROOT_SOUP,COOKED_CHICKEN,COOKED_MUTTON,COOKED_SALMON,GOLDEN_CARROT,
                 HONEY_BLOCK,MUSHROOM_STEW,SUSPICIOUS_STEW -> 6;
            case COOKED_PORKCHOP,PUMPKIN_PIE,COOKED_BEEF -> 8;
            case RABBIT_STEW -> 10;
            default -> 1;
        };
    }
}