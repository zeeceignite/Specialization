package com.minecraftcivilizations.specialization.Mining;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.minecraftcivilizations.specialization.Config.Config;
import com.minecraftcivilizations.specialization.Listener.PlayerMineListener;
import com.minecraftcivilizations.specialization.Specialization;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Random;

public class BlockDamage {
    private static ProtocolManager manager = ProtocolLibrary.getProtocolManager();

    public PacketContainer configureBreakingPacket(Player player, Block block) {
        PacketContainer breakingAnimation = manager.createPacket(PacketType.Play.Server.BLOCK_BREAK_ANIMATION);

        int entityId = player.getEntityId() + 1;
        entityId = entityId * 1000;

        breakingAnimation.getIntegers().write(0, entityId);
        breakingAnimation.getBlockPositionModifier().write(0, new BlockPosition(block.getX(), block.getY(), block.getZ()));

        return breakingAnimation;
    }

    public void startBreaking(Player player, PacketContainer breakingAnimation, double breakingTimeTicks, Block originalBlock) {

        new BukkitRunnable() {
            double currentTicks = 0d;

            @Override
            public void run() {
                // stops breaking if player isn't actively breaking the block
                if (!(PlayerMineListener.armSwinging.containsKey(player.getName()))) {
                    this.cancel();
                    // returns the breaking animation back to none
                    breakingAnimation.getIntegers().write(1, -1);
                    manager.sendServerPacket(player, breakingAnimation);
                    return;
                }

                Block currentTarget = player.getTargetBlockExact(5);

                // removes any progress if mining from block onto air and cancels this task
                if (currentTarget == null) {
                    this.cancel();

                    // returns the breaking animation back to none
                    breakingAnimation.getIntegers().write(1, -1);
                    manager.sendServerPacket(player, breakingAnimation);
                    return;
                }

                // breaks the block if it has been mined for a succificnet amount of time
                if(currentTicks >= breakingTimeTicks) {
                    // sets the final breaking animation
                    breakingAnimation.getIntegers().write(1, 9);
                    manager.sendServerPacket(player, breakingAnimation);

                    playerBreakBlock(player, originalBlock);
                    breakingAnimation.getIntegers().write(1, -1);
                    this.cancel();
                    return;
                } else {
                    double multiplier = 0.1;
                    for (int x=0; x <= 9; x++) {
                        if (currentTicks <= (breakingTimeTicks * multiplier)) {
                            breakingAnimation.getIntegers().write(1,  x-1);
                            manager.sendServerPacket(player, breakingAnimation);
                            Specialization.logger.info("Current Ticks: " + currentTicks);
                            Specialization.logger.info("Progress: " + (x-1));
                            break;
                        }
                        multiplier += 0.1;
                    }
                }
                currentTicks ++;
            }
        }.runTaskTimer(Specialization.getInstance(), 0L, 1L);
    }

    public double getBreakingTime(Player player, Block block) {
        double speedMultiplier = 1d;


        ItemStack item = player.getEquipment().getItemInMainHand();

        if (block.isPreferredTool(player.getEquipment().getItemInMainHand())) {

            if (item.getType().equals(Material.WOODEN_PICKAXE) ||
                    item.getType().equals(Material.WOODEN_SHOVEL) ||
                    item.getType().equals(Material.WOODEN_AXE) ||
                    item.getType().equals(Material.WOODEN_HOE)) speedMultiplier = 2d;

            else if (item.getType().equals(Material.STONE_PICKAXE) ||
                    item.getType().equals(Material.STONE_SHOVEL) ||
                    item.getType().equals(Material.STONE_AXE) ||
                    item.getType().equals(Material.STONE_HOE)) speedMultiplier = 4d;

            else if (item.getType().equals(Material.IRON_PICKAXE) ||
                    item.getType().equals(Material.IRON_SHOVEL) ||
                    item.getType().equals(Material.IRON_AXE) ||
                    item.getType().equals(Material.IRON_HOE)) speedMultiplier = 6d;

            else if (item.getType().equals(Material.DIAMOND_PICKAXE) ||
                    item.getType().equals(Material.DIAMOND_SHOVEL) ||
                    item.getType().equals(Material.DIAMOND_AXE) ||
                    item.getType().equals(Material.DIAMOND_HOE)) speedMultiplier = 8d;

            else if (item.getType().equals(Material.NETHERITE_PICKAXE) ||
                    item.getType().equals(Material.NETHERITE_SHOVEL) ||
                    item.getType().equals(Material.NETHERITE_AXE) ||
                    item.getType().equals(Material.NETHERITE_HOE)) speedMultiplier = 9d;

            else if (item.getType().equals(Material.GOLDEN_PICKAXE) ||
                    item.getType().equals(Material.GOLDEN_SHOVEL) ||
                    item.getType().equals(Material.GOLDEN_AXE) ||
                    item.getType().equals(Material.GOLDEN_HOE)) speedMultiplier = 12d;

            if (item.hasItemMeta()) {
                if (item.getItemMeta().hasEnchant(Enchantment.EFFICIENCY)) {
                    speedMultiplier += Math.pow(item.getEnchantmentLevel(Enchantment.EFFICIENCY), 2) + 1d;
                }
            }

        }


        if (player.hasPotionEffect(PotionEffectType.HASTE)) {
            speedMultiplier *= 1 + (0.2 * player.getPotionEffect(PotionEffectType.HASTE).getAmplifier());
        }

        if (player.isInWater()) {
            speedMultiplier /= 5;
        }

        if (!player.isOnGround()) {
            speedMultiplier /= 5;
        }

        double damage;

        damage = speedMultiplier / Config.getBlockHardnessConfig().getDouble(block.getType().name());;

        damage /= 30;

        // Instant breaking
        if (damage > 1) {
            return 0d;
        }

        return Math.round(1 / damage);
    }

    public void playerBreakBlock(Player player, Block block) {


        block.breakNaturally(player.getEquipment().getItemInMainHand(), true, true);
        block.getWorld().playSound(block.getLocation(), block.getBlockData().getSoundGroup().getBreakSound(), 1.0f, 1.0f);

        ItemStack item = player.getEquipment().getItemInMainHand();
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return;
        }
        if (meta.isUnbreakable()) {
            return;
        }

        if (meta.hasEnchant(Enchantment.UNBREAKING)) {
            Random random = new Random();

            if (random.nextInt(Math.round(100 / (meta.getEnchantLevel(Enchantment.UNBREAKING) + 1))) == 0) {
                return;
            }
        }
    }
}