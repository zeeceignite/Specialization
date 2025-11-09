package com.minecraftcivilizations.specialization.CustomItem;

import com.minecraftcivilizations.specialization.Specialization;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class DefineCustomItems {

    // Existing items
    CustomItem bandage = new Bandage("bandage", "Bandage");
    CustomItem blessed_food = new BlessedFood("blessed_food");

    CustomItem cool_sword = new CustomItem("cool_sword", "Cool Sword", Material.DIAMOND_SWORD, "cool_sword") {

        @Override
        public void init() {}

        @Override
        public void onCreateItem(ItemStack itemStack, ItemMeta meta, Player player) {
            meta.setEnchantmentGlintOverride(true);
        }

        @Override
        public void onInteract(PlayerInteractEvent event, ItemStack itemStack) {
            event.getPlayer().getWorld().spawnParticle(Particle.CLOUD, event.getPlayer().getLocation(), 100, 0.2f, 0.2f, 0.2f);
            event.getPlayer().playSound(event.getPlayer(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1, 1);
        }
    };


}
