package com.minecraftcivilizations.specialization.Listener.Animal;

import com.minecraftcivilizations.specialization.Animal.AnimalFeedingManager;
import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import org.bukkit.entity.Animals;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

public class AnimalFeedListener implements Listener {

    @EventHandler
    public void onPlayerFeedAnimal(PlayerInteractEntityEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (!(event.getRightClicked() instanceof Animals animal)) return;
        Boolean requiresFeeding = SpecializationConfig.getAnimalFeedingConfig().get("REQUIRE_FEEDING_" + animal.getType().name(), Boolean.class);
        if (requiresFeeding == null || !requiresFeeding) return;
        var itemInHand = event.getPlayer().getInventory().getItemInMainHand();
        if (itemInHand.isEmpty()) return;
        if (!AnimalFeedingManager.isAnimalFood(animal, itemInHand.getType())) return;

        event.setCancelled(true);

        boolean shouldConsume = AnimalFeedingManager.handleAnimalFed(animal, itemInHand.getType());
        if (shouldConsume) {
            itemInHand.setAmount(itemInHand.getAmount() - 1);
        }
    }
}
