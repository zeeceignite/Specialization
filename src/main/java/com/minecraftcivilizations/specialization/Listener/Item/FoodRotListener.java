package com.minecraftcivilizations.specialization.Listener.Item;

import com.minecraftcivilizations.specialization.Food.FoodRotManager;
import com.minecraftcivilizations.specialization.Specialization;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

public class FoodRotListener implements Listener {

    private final Specialization plugin;
    private BukkitTask updateTask;

    public FoodRotListener(Specialization plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        startPeriodicUpdate();
    }


    private void startPeriodicUpdate() {
        long updateIntervalTicks = 20L * 60L;

        updateTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                // Schedule the actual inventory update on the main thread since inventory operations aren't thread-safe
                Bukkit.getScheduler().runTask(plugin, () -> {
                    FoodRotManager.updateInventoryFoodLore(player);
                });
            }
        }, updateIntervalTicks, updateIntervalTicks);
    }

    public void stopPeriodicUpdate() {
        if (updateTask != null) {
            updateTask.cancel();
        }
    }

    @EventHandler
    public void onFoodPickup(PlayerPickupItemEvent event) {
        ItemStack item = event.getItem().getItemStack();
        if (item != null && item.getType().isEdible()) {
            FoodRotManager.stampFoodCreationTime(item);
        }
    }

    @EventHandler
    public void onFoodCraft(CraftItemEvent event) {
        ItemStack crafted = event.getCurrentItem();
        if (crafted != null && crafted.getType().isEdible()) {
            FoodRotManager.stampFoodCreationTime(crafted);
        }
    }

    @EventHandler
    public void onFoodSmelt(FurnaceExtractEvent event) {
        ItemStack extracted = new ItemStack(event.getItemType(), event.getItemAmount());
        if (extracted.getType().isEdible()) {
            if (event.getBlock().getType() == org.bukkit.Material.SMOKER) {
                FoodRotManager.markFoodAsSmoked(extracted);
            }
            FoodRotManager.stampFoodCreationTime(extracted);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            FoodRotManager.updateInventoryFoodLore((Player) event.getWhoClicked());
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            FoodRotManager.updateInventoryFoodLore((Player) event.getWhoClicked());
        }
    }
}