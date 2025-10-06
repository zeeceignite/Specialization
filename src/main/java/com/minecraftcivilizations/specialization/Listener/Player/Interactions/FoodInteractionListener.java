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
        CustomItem ci = new CustomItem(item.getType(),
                Component.text("Blessed " + pretty).color(NamedTextColor.GOLD));

        String summary;
        if (healerLevel >= SkillLevel.GRANDMASTER.getLevel()) {
            summary = "Regeneration III 20s, Absorption II 40s";
        } else if (healerLevel >= SkillLevel.MASTER.getLevel()) {
            summary = "Regeneration III 14s, Absorption I 40s";
        } else if (healerLevel >= SkillLevel.EXPERT.getLevel()) {
            summary = "Regeneration II 25s";
        } else {
            summary = "Regeneration I 30s";
        }

        ci.addLore(Specialization.getInstance(), List.of(
                Component.empty(),
                Component.text("Blessed Food").color(NamedTextColor.YELLOW),
                Component.text("Healer Level: " + healerLevel).color(NamedTextColor.GRAY),
                Component.text(summary).color(NamedTextColor.GRAY)
        ));
        item.setItemMeta(ci.getItem().getItemMeta());
    }




    // --- Bless values (seconds) ---
    private static final int L2_REGEN_SEC = 30; // Regen I
    private static final int L3_REGEN_SEC = 25; // Regen II
    private static final int L4_REGEN_SEC = 14; // Regen III (short, strong)
    private static final int L4_ABSORB_SEC = 40; // Abs I
    private static final int L5_REGEN_SEC = 20; // Regen III
    private static final int L5_ABSORB_SEC = 40; // Abs II
// ------------------------------

    private void applyBlessedFoodEffects(Player player, int healerLevel, Material itemType) {
        int regenTicks = 0;
        int regenAmp = 0; // 0=Regen I, 1=Regen II, 2=Regen III
        Integer absTicks = null, absAmp = null;

        if (healerLevel >= SkillLevel.GRANDMASTER.getLevel()) {
            regenTicks = L5_REGEN_SEC * 20; regenAmp = 2; // Regen III
            absTicks = L5_ABSORB_SEC * 20;  absAmp = 1;   // Abs II
        } else if (healerLevel >= SkillLevel.MASTER.getLevel()) {
            regenTicks = L4_REGEN_SEC * 20; regenAmp = 2; // Regen III (shorter)
            absTicks = L4_ABSORB_SEC * 20;  absAmp = 0;   // Abs I
        } else if (healerLevel >= SkillLevel.EXPERT.getLevel()) {
            regenTicks = L3_REGEN_SEC * 20; regenAmp = 1; // Regen II
        } else if (healerLevel >= SkillLevel.JOURNEYMAN.getLevel()) {
            regenTicks = L2_REGEN_SEC * 20; regenAmp = 0; // Regen I
        } else {
            return; // cannot bless
        }

        if (regenTicks > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, regenTicks, regenAmp));
        }
        if (absTicks != null) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, absTicks, absAmp));
        }

        // keep your existing max-health-restore block:
        if (SpecializationConfig.getHealthConfig().get("HEALTH_ENABLED", Boolean.class)) {
            double currentMaxHealth = Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getValue();
            double normalMaxHealth  = SpecializationConfig.getHealthConfig().get("MAX_HEALTH", Double.class);
            double restore          = SpecializationConfig.getHealthConfig().get("BLESSED_FOOD_HEALTH_RESTORE_AMOUNT", Double.class);
            if (currentMaxHealth < normalMaxHealth) {
                double newMax = Math.min(normalMaxHealth, currentMaxHealth + restore);
                Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(newMax);
                player.sendMessage(ChatColor.GREEN + "You feel your vitality returning! Max health restored to " + (int)newMax);
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