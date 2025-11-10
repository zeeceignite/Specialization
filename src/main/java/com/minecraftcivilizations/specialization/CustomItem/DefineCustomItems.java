package com.minecraftcivilizations.specialization.CustomItem;

import com.minecraftcivilizations.specialization.Combat.ArmorEquipAttributes;
import com.minecraftcivilizations.specialization.Specialization;
import com.minecraftcivilizations.specialization.StaffTools.Debug;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

/**
 *
 * @author  alectriciti, jfrogy
 */
public class DefineCustomItems implements Listener {



    public DefineCustomItems(Specialization plugin){
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // intercept the take from the result slot and replace with your modified item
    @EventHandler(priority = EventPriority.HIGHEST) //Highest will run AFTER the Xp is given
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getSlotType() != InventoryType.SlotType.RESULT) return;
        if(event.isCancelled())return;
        if(event.getResult()!=Event.Result.ALLOW)return;

        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType() == Material.AIR) return;

        ItemStack modified = current.clone();
        ItemMeta meta = modified.getItemMeta();

        if (current.getType().name().contains("_SWORD")) {
            // final tweak applied when player actually takes the item
            masterwork_sword.wrapItemStack(modified, (Player) event.getWhoClicked());
            // setCurrentItem changes what the player receives from the result slot
            event.setCurrentItem(modified);
        }
    }

    /**
     * Vanilla Overrides
     */
        @EventHandler(ignoreCancelled = true)
        public void onCraftModifyName(CraftItemEvent event) {
//            if (!(event.getWhoClicked() instanceof Player player)) return;
//
//            ItemStack current = event.getCurrentItem();
//            if (current == null) return;
//
//            if (!current.getType().name().contains("_SWORD")) return;
//            ItemStack cursor = event.getCursor();
//
//            Debug.broadcast("craft",
//                    "<gray>current:</gray> "+current.getType().name()
//                    +"<gray>cursor:</gray> "+cursor.getType().name());
//
//            Bukkit.getScheduler().runTask(Specialization.getInstance(), () -> {
//                handleCustomSword(player, cursor, current);
//            });
        }
//
//    private static void handleCustomSword(Player player, ItemStack cursor, ItemStack result) {
//        // Attempt to give to cursor first (normal click)
//        if (cursor != null && cursor.getType() == result.getType()) {
//            ItemMeta meta = cursor.getItemMeta();
//            if (meta != null) {
//                meta.displayName(Component.text("Masterwork " + result.getType().name()));
//                cursor.setItemMeta(meta);
//            }
//            return;
//        }
//
//        // Handle shift-click (inventory)
//        boolean given = false;
//        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
//            ItemStack invItem = player.getInventory().getItem(slot);
//            if (invItem == null) continue;
//            if (invItem.getType() != result.getType()) continue;
//
//            ItemMeta meta = invItem.getItemMeta();
//            if (meta != null && meta.displayName() == null) {
//                meta.displayName(Component.text("Masterwork " + result.getType().name()));
//                invItem.setItemMeta(meta);
//                given = true;
//                break;
//            }
//        }
//
//        // If we didn’t modify any inventory slot, drop the item at the player
//        if (!given) {
//            ItemStack dropped = result.clone();
//            ItemMeta meta = dropped.getItemMeta();
//            if (meta != null) meta.displayName(Component.text("Masterwork " + result.getType().name()));
//            dropped.setItemMeta(meta);
//            player.getWorld().dropItemNaturally(player.getLocation(), dropped);
//        }
//    }


    CustomItem bandage = new Bandage("bandage", "Bandage");
    CustomItem blessed_food = new BlessedFood("blessed_food");

    CustomItem masterwork_sword = new CustomWeapon("masterwork_sword");

    CustomItem cool_sword = new CustomItem("cool_sword", "Cool Sword", Material.DIAMOND_SWORD, "cool_sword"){

    // Example Sword
    CustomItem cool_sword = new CustomItem("cool_sword", "Cool Sword", Material.DIAMOND_SWORD, "cool_sword", false) {
        @Override
        public void init() {}
        @Override
        public void onCreateItem(ItemStack itemStack, ItemMeta meta, Player player_who_crafted) {
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
