package com.minecraftcivilizations.specialization.Mining;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.google.gson.Gson;
import com.minecraftcivilizations.specialization.Config.Config;
import com.minecraftcivilizations.specialization.Listener.PlayerMineListener;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Recipe.Pair;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.Specialization;
import minecraftcivilizations.com.minecraftCivilizationsCore.MinecraftCivilizationsCore;
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
import java.util.Set;

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
                Block currentBlock = player.getTargetBlockExact(5);


                if (!PlayerMineListener.armSwinging.containsKey(player.getName()) || currentBlock == null || !currentBlock.equals(originalBlock)) {
                    this.cancel();
                    // returns the breaking animation back to none
                    breakingAnimation.getIntegers().write(1, -1);
                    manager.sendServerPacket(player, breakingAnimation);
                    return;
                }

                // breaks the block if it has been mined for a sufficient amount of time
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
            if (item.getType().name().matches("WOODEN_(PICKAXE|SHOVEL|AXE|HOE)")) speedMultiplier = 2d;

            else if (item.getType().name().matches("STONE_(PICKAXE|SHOVEL|AXE|HOE)")) speedMultiplier = 4d;

            else if (item.getType().name().matches("IRON_(PICKAXE|SHOVEL|AXE|HOE)")) speedMultiplier = 6d;

            else if (item.getType().name().matches("DIAMOND_(PICKAXE|SHOVEL|AXE|HOE)")) speedMultiplier = 8d;

            else if (item.getType().name().matches("NETHERITE_(PICKAXE|SHOVEL|AXE|HOE)")) speedMultiplier = 9d;

            else if (item.getType().name().matches("GOLDEN_(PICKAXE|SHOVEL|AXE|HOE)")) speedMultiplier = 12d;

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
        CustomPlayer customPlayer = (CustomPlayer) MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().getCustomPlayer(player.getUniqueId());
        Pair pair = new Gson().fromJson(Config.getXpGainFromBreakingConfig().getString(block.getType().name()), Pair.class);
        Specialization.logger.info(block.getType().name() + " ");
        customPlayer.addSkillXp(SkillType.valueOf(pair.key()), Double.parseDouble(pair.value()));

        block.breakNaturally(player.getEquipment().getItemInMainHand(), true, true);
    }
}