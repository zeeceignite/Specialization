package com.minecraftcivilizations.specialization.Listener.Player.Interactions;

import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Reinforcement.ReinforcementManager;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
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
        if(event.getClickedBlock() == null) return;
        if (ReinforcementManager.isReinforced(event.getClickedBlock())) return;

        Player player = event.getPlayer();
        CustomPlayer cPlayer = CoreUtil.getPlayer(player);
        if(event.getClickedBlock().getType().equals(Material.SWEET_BERRY_BUSH) && cPlayer.getSkillLevel(SkillType.FARMER) < 2){
            if(Math.random() < 0.2){
                player.damage(1);
                player.sendRichMessage("<#d16060>Ouch! You pricked your finger on the berry bush.");
            }
        }

        if (player.getInventory().getItemInMainHand().getType() == Material.COPPER_INGOT) {
            if (ReinforcementManager.addReinforcement(event.getClickedBlock(), false)) {
                player.swingHand(EquipmentSlot.HAND);
                player.getInventory().getItemInMainHand().setAmount(player.getInventory().getItemInMainHand().getAmount() - 1);
                player.sendMessage(Component.text("Block now lightly reinforced!").color(NamedTextColor.WHITE).decorations(Set.of(TextDecoration.BOLD, TextDecoration.ITALIC), false));
            }
        } else if (player.getInventory().getItemInMainHand().getType() == Material.IRON_INGOT) {
            if (ReinforcementManager.addReinforcement(event.getClickedBlock(), true)) {
                player.swingHand(EquipmentSlot.HAND);
                player.getInventory().getItemInMainHand().setAmount(player.getInventory().getItemInMainHand().getAmount() - 1);
                player.sendMessage(Component.text("Block now heavily reinforced!").color(NamedTextColor.WHITE).decorations(Set.of(TextDecoration.BOLD, TextDecoration.ITALIC), false));
            }
        }
    }
}
