package com.minecraftcivilizations.specialization.Listener.Player.Interactions;

import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Reinforcement.ReinforcementManager;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import com.minecraftcivilizations.specialization.util.PlayerUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Door;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RightClickListener implements Listener {
    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (!(event.getAction() == Action.RIGHT_CLICK_BLOCK)) return;
        if(event.getClickedBlock() == null) return;
        if(event.getClickedBlock().getType().equals(Material.SPAWNER)){
            event.setCancelled(true);
        }

        if (ReinforcementManager.isReinforced(event.getClickedBlock())) return;

        Player player = event.getPlayer();
        CustomPlayer cPlayer = CoreUtil.getPlayer(player);
        if(event.getClickedBlock().getType().equals(Material.SWEET_BERRY_BUSH) && cPlayer.getSkillLevel(SkillType.FARMER) < 2){
            if(Math.random() < 0.2){
                player.damage(1);
//                player.chat("Ouch!");
            }
        }

        if (player.getInventory().getItemInMainHand().getType() == Material.COPPER_INGOT) {
            CustomPlayer customPlayer = CoreUtil.getPlayer(player.getUniqueId());
            if (customPlayer.getSkillLevel(SkillType.BUILDER) >= SpecializationConfig.getReinforcementConfig().get("LIGHT_REINFORCEMENT_LEVEL", Integer.class)) {
                List<Block> blocks = getMultiBlocks(event.getClickedBlock());
                boolean success = false;
                for (Block block : blocks) {
                    if (ReinforcementManager.addReinforcement(player, block, false)) {
                        success = true;
                    }
                }
                if (success) {
                    player.swingHand(EquipmentSlot.HAND);
                    player.getInventory().getItemInMainHand().setAmount(player.getInventory().getItemInMainHand().getAmount() - 1);
                    PlayerUtil.message(player, Component.text("§7Lightly Reinforced").color(NamedTextColor.WHITE).decorations(Set.of(TextDecoration.BOLD, TextDecoration.ITALIC), false));
                }
            }
        } else if (player.getInventory().getItemInMainHand().getType() == Material.IRON_INGOT) {
            CustomPlayer customPlayer = CoreUtil.getPlayer(player.getUniqueId());

            if (customPlayer.getSkillLevel(SkillType.BUILDER) >= SpecializationConfig.getReinforcementConfig().get("HEAVY_REINFORCEMENT_LEVEL", Integer.class)) {
                List<Block> blocks = getMultiBlocks(event.getClickedBlock());
                boolean success = false;
                for (Block block : blocks) {
                    if (ReinforcementManager.addReinforcement(player, block, true)) {
                        success = true;
                    }
                }
                if (success) {
                    player.swingHand(EquipmentSlot.HAND);
                    player.getInventory().getItemInMainHand().setAmount(player.getInventory().getItemInMainHand().getAmount() - 1);
                    PlayerUtil.message(player, Component.text("§7Heavily Reinforced").color(NamedTextColor.WHITE).decorations(Set.of(TextDecoration.BOLD, TextDecoration.ITALIC), false));
                }
            }
        }

    }

    //force feed players as guardsmen
    @EventHandler(priority = EventPriority.HIGH)
    public void onRightClickPlayer(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player target)) return;

        Player player = event.getPlayer();
        CustomPlayer cPlayer = CoreUtil.getPlayer(player);

        // Guardsman/healer check
        if ((cPlayer.getSkillLevel(SkillType.GUARDSMAN) <= 0) && (cPlayer.getSkillLevel(SkillType.HEALER) <= 2)) return;

        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (!handItem.getType().isEdible()) return;

        // Force feed: add 1 hunger
        // Only feed if target is not full
        if (!(target.getFoodLevel() < 20)) {
            PlayerUtil.message(player, target.getName() + " cant handle more food", 1);
           return;
        }

        target.setFoodLevel(Math.min(target.getFoodLevel() + 2, 20));
        // Play swing animation
        ItemStack held = player.getInventory().getItemInMainHand();

        player.swingHand(EquipmentSlot.HAND);

        float pitch = 0.8f + (float) (Math.random() * 0.4f); // 0.8–1.2
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 1f, pitch);

        target.getWorld().spawnParticle(
                Particle.ITEM,
                target.getEyeLocation(),
                8,
                0.2, 0, 0.5,
                0,
                held
        );

        // Consume one item from hand
        handItem.setAmount(handItem.getAmount() - 1);
//        PlayerUtil.message(player, "Force fed <gold>" + target.getName(), 1);
//        PlayerUtil.message(target, "<gold>" + target.getName() + "</gold>force fed you", 1);
    }


    private List<Block> getMultiBlocks(Block block) {
        List<Block> blocks = new ArrayList<>();
        blocks.add(block);
        if (block.getBlockData() instanceof Door door) {
            blocks.add(block.getRelative(door.getHalf() == Bisected.Half.TOP ? BlockFace.DOWN : BlockFace.UP));
        } else if (block.getBlockData() instanceof Bed bed) {
            blocks.add(block.getRelative(bed.getPart() == Bed.Part.HEAD ? bed.getFacing().getOppositeFace() : bed.getFacing()));
        } else if (block.getBlockData() instanceof Bisected bisected) {
            blocks.add(block.getRelative(bisected.getHalf() == Bisected.Half.TOP ? BlockFace.DOWN : BlockFace.UP));
        }
        return blocks;
    }
}
