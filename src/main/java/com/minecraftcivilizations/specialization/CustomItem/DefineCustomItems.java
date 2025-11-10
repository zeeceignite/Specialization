package com.minecraftcivilizations.specialization.CustomItem;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class DefineCustomItems {

    // === Existing Items ===
    CustomItem bandage = new Bandage("bandage", "Bandage");

    // Example Sword
    CustomItem cool_sword = new CustomItem("cool_sword", "Cool Sword", Material.DIAMOND_SWORD, "cool_sword", false) {
        @Override
        public void init() {}
        @Override
        public void onCreateItem(ItemStack itemStack, ItemMeta meta, Player player) {
            meta.setEnchantmentGlintOverride(true);
        }
        @Override
        public void onInteract(PlayerInteractEvent event, ItemStack itemStack) {
            event.getPlayer().getWorld().spawnParticle(
                    org.bukkit.Particle.CLOUD,
                    event.getPlayer().getLocation(), 100, 0.2f, 0.2f, 0.2f
            );
            event.getPlayer().playSound(event.getPlayer(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1, 1);
        }
    };

    // === Generic Blessed Food (light regen) ===
    BlessedFood blessed_food = new BlessedFood(
            "blessed_food",
            "§eBlessed Food",
            Material.COOKED_BEEF,
            PotionEffectType.REGENERATION,
            20 * 6, 1, 200,
            List.of(Material.COOKED_BEEF, Material.SUGAR)
    );

    // === Hearty Soup (stronger regen, shapeless) ===
    BlessedFood hearty_soup = new BlessedFood(
            "hearty_soup",
            "§6Hearty Soup",
            Material.BEETROOT_SOUP,
            PotionEffectType.REGENERATION,
            20 * 10, 1, 400,
            List.of(Material.FERMENTED_SPIDER_EYE, Material.BOWL)
    ) {
        @Override
        public void onCreateItem(ItemStack itemStack, ItemMeta meta, Player player) {
            meta.setLore(List.of(
                    "§7A warm soup imbued with divine vitality.",
                    "§eRestores health and grants powerful regeneration."
            ));
            itemStack.setItemMeta(meta);
        }
    };

    // === Radiant Bread (speed boost, shapeless) ===
    BlessedFood radiant_bread = new BlessedFood(
            "radiant_bread",
            "§fRadiant Bread",
            Material.BREAD,
            PotionEffectType.SPEED,
            20 * 15, 1, 300,
            List.of(Material.WHEAT, Material.HONEY_BOTTLE)
    ) {
        @Override
        public void onCreateItem(ItemStack itemStack, ItemMeta meta, Player player) {
            meta.setLore(List.of(
                    "§7A loaf infused with radiant energy.",
                    "§eGrants a burst of speed when eaten."
            ));
            itemStack.setItemMeta(meta);
        }
    };
}
