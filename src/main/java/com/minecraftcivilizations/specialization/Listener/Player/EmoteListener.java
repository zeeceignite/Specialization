package com.minecraftcivilizations.specialization.Listener.Player;

import io.papermc.paper.event.entity.EntityLoadCrossbowEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class EmoteListener implements Listener {

    private final JavaPlugin plugin;

    public EmoteListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean isPointCrossbow(ItemStack item) {
        if (item == null) return false;
        if (!(item.getItemMeta() instanceof CrossbowMeta meta)) return false;
        return meta.hasCustomModelData() && meta.getCustomModelData() == 420;
    }

    private boolean isClapCrossbow(ItemStack item) {
        if (item == null) return false;
        if (!(item.getItemMeta() instanceof CrossbowMeta meta)) return false;
        return meta.hasCustomModelData() && meta.getCustomModelData() == 69;
    }

    // Track crossbow directly
    private boolean isSpecialCrossbow(ItemStack item) {
        return isClapCrossbow(item) || isPointCrossbow(item);
    }

    @EventHandler
    public void onCrossbowInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (!isSpecialCrossbow(item)) return;
        // Only handle “use item” actions
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();

        // Play sound once

        // Configurable ranges
        float minVolume1 = 0.9f, maxVolume1 = 1.1f;
        float minPitch1 = 1.8f, maxPitch1 = 2.2f;

        float minVolume2 = 0.7f, maxVolume2 = 0.9f;
        float minPitch2 = 1.6f, maxPitch2 = 2.0f;

        // Give temporary arrow if none
        if (!player.getInventory().contains(Material.ARROW)) {
            int arrowSlot = 41; // fake slot
            ItemStack arrow = new ItemStack(Material.ARROW);
            player.getInventory().setItem(arrowSlot, arrow);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.getInventory().setItem(arrowSlot, null);

                if (player.isHandRaised()) {
                    // Randomized values
                    float volume1 = minVolume1 + (float)Math.random() * (maxVolume1 - minVolume1);
                    float pitch1 = minPitch1 + (float)Math.random() * (maxPitch1 - minPitch1);
                    player.playSound(player.getLocation(), Sound.BLOCK_WOOD_HIT, SoundCategory.PLAYERS, volume1, pitch1);

                    float volume2 = minVolume2 + (float)Math.random() * (maxVolume2 - minVolume2);
                    float pitch2 = minPitch2 + (float)Math.random() * (maxPitch2 - minPitch2);
                    player.playSound(player.getLocation(), Sound.BLOCK_WOOD_HIT, SoundCategory.PLAYERS, volume2, pitch2);
                }
            }, 1L);
        }
    }

    @EventHandler
    public void onLoadCrossbow(EntityLoadCrossbowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isSpecialCrossbow(event.getCrossbow())) return;

        event.setCancelled(true);
        event.setConsumeItem(false);
    }

    @EventHandler
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isSpecialCrossbow(event.getBow())) return;

        event.setCancelled(true);
    }

    // Prevent moving/dropping: remove the actual crossbow instance
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (!isSpecialCrossbow(clicked)) return;

        if (event.getWhoClicked() instanceof Player player) {
            // Find the exact ItemStack in inventory and remove
            player.getInventory().remove(clicked);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onHotbarSwitch(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack previous = player.getInventory().getItem(event.getPreviousSlot());
        if (isSpecialCrossbow(previous)) {
            player.getInventory().remove(previous);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack dropped = event.getItemDrop().getItemStack();
        if (isSpecialCrossbow(dropped)) {
            event.getItemDrop().remove(); // remove the entity immediately
            event.setCancelled(true); // prevent it actually dropping
        }
    }
}
