package com.minecraftcivilizations.specialization.Listener.Player;

import com.minecraftcivilizations.specialization.Specialization;
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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

public class EmoteListener implements Listener {

    private final Map<Player, BukkitTask> pendingReloads = new HashMap<>();

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


    // Prevent shooting completely
    @EventHandler
    public void onCrossbowInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null) return;
        if (!isClapCrossbow(item) && !isPointCrossbow(item)) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
       Player player = event.getPlayer();

       player.playSound(player.getLocation(), Sound.BLOCK_WOOD_HIT, SoundCategory.PLAYERS, 1f, 1f);
}



    @EventHandler
    public void onLoadCrossbow(EntityLoadCrossbowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack item = event.getCrossbow();
        if (!isClapCrossbow(item)) return;

        event.setCancelled(true);         // prevent actual completion
        event.setConsumeItem(false);      // do not consume projectile

        if (!(item.getItemMeta() instanceof CrossbowMeta meta)) return;

        // immediately unload
        meta.setChargedProjectiles(null);
        item.setItemMeta(meta);

    }

    @EventHandler
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack item = event.getBow();
        if (isClapCrossbow(item) || isPointCrossbow(item)) {
            event.setCancelled(true);
        }

    }
}
