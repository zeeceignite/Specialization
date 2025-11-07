package com.minecraftcivilizations.specialization.CustomItem;

import com.minecraftcivilizations.specialization.Specialization;
import com.minecraftcivilizations.specialization.StaffTools.Debug;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
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

        if (!current.getType().name().contains("_SWORD")) return;

        ItemStack modified = current.clone();
        ItemMeta meta = modified.getItemMeta();
        // final tweak applied when player actually takes the item
        masterwork_sword.wrapItemStack(modified, (Player) event.getWhoClicked());

        // setCurrentItem changes what the player receives from the result slot
        event.setCurrentItem(modified);
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
//    CustomItem bandage_super = new Bandage("bandage_super", "Bandage");


    CustomItem blessed_food = new BlessedFood("blessed_food");

    CustomItem masterwork_sword = new CustomWeapon("masterwork_sword");

    CustomItem cool_sword = new CustomItem("cool_sword", "Cool Sword", Material.DIAMOND_SWORD, "cool_sword"){

        @Override
        public void init() {
            
        }

        @Override
        public void onCreateItem(ItemStack itemStack, ItemMeta meta, Player player_who_crafted) {
            meta.setEnchantmentGlintOverride(true);
        }

        @Override
        public void onInteract(PlayerInteractEvent event, ItemStack itemStack) {
            event.getPlayer().getWorld().spawnParticle(Particle.CLOUD, event.getPlayer().getLocation(), 100, 0.2f, 0.2f, 0.2f);
            event.getPlayer().playSound(event.getPlayer(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1, 1);
        }
    };


//    public void reloadCustomItems() {
//        Specialization.getInstance().getLogger().info("Reloaded all custom items.");
//    }



}
