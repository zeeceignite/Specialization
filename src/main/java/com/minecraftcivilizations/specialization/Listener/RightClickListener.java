package com.minecraftcivilizations.specialization.Listener;

import com.minecraftcivilizations.specialization.Reinforcement.ReinforcementManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Set;

public class RightClickListener implements Listener {
    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (!(event.getAction() == Action.RIGHT_CLICK_BLOCK)) return;
        if (ReinforcementManager.isReinforced(event.getClickedBlock())) return;

        if (event.getPlayer().getInventory().getItemInMainHand().getType() == Material.IRON_NUGGET) {
            if (ReinforcementManager.addReinforcement(event.getClickedBlock(), false)) {
                event.getPlayer().swingHand(EquipmentSlot.HAND);
                event.getPlayer().getInventory().getItemInMainHand().setAmount(event.getPlayer().getInventory().getItemInMainHand().getAmount() - 1);
                event.getPlayer().sendMessage(Component.text("Block now lightly reinforced!").color(NamedTextColor.WHITE).decorations(Set.of(TextDecoration.BOLD, TextDecoration.ITALIC), false));
            }
        } else if (event.getPlayer().getInventory().getItemInMainHand().getType() == Material.IRON_INGOT) {
            if (ReinforcementManager.addReinforcement(event.getClickedBlock(), true)) {
                event.getPlayer().swingHand(EquipmentSlot.HAND);
                event.getPlayer().getInventory().getItemInMainHand().setAmount(event.getPlayer().getInventory().getItemInMainHand().getAmount() - 1);
                event.getPlayer().sendMessage(Component.text("Block now heavily reinforced!").color(NamedTextColor.WHITE).decorations(Set.of(TextDecoration.BOLD, TextDecoration.ITALIC), false));
            }
        }
    }
}
