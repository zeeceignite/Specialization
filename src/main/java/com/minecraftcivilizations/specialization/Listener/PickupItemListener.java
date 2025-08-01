package com.minecraftcivilizations.specialization.Listener;

import com.minecraftcivilizations.specialization.Specialization;
import minecraftcivilizations.com.minecraftCivilizationsCore.Item.CustomItem;
import minecraftcivilizations.com.weight.API.WeightAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Set;

public class PickupItemListener implements Listener {
    @EventHandler(ignoreCancelled = true)
    public void onPickupItem(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            ItemStack item = event.getItem().getItemStack();
            CustomItem customItem = new CustomItem();
            customItem.setItem(item);
            customItem.reloadItem();
            try {
                customItem.addLore(Specialization.getInstance(), List.of(Component.text(WeightAPI.getWeightOfItem(item)).color(NamedTextColor.WHITE).decorations(Set.of(TextDecoration.OBFUSCATED, TextDecoration.ITALIC), false)));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        event.setCancelled(true);
    }
}
