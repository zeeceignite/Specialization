package com.minecraftcivilizations.specialization.Mining;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Listener.PlayerMineListener;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Reinforcement.ReinforcementManager;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.Specialization;
import minecraftcivilizations.com.minecraftCivilizationsCore.MinecraftCivilizationsCore;
import minecraftcivilizations.com.minecraftCivilizationsCore.Options.Pair;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import static com.minecraftcivilizations.specialization.Skill.Skill.mapValue;

public class BlockDamage {
    private static final ProtocolManager manager = ProtocolLibrary.getProtocolManager();

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
            final double ticksPerState = breakingTimeTicks / 9d;
            double currentTicks = 0d;

            @Override
            public void run() {
                // stops breaking if player isn't actively breaking the block
                Block currentBlock = player.getTargetBlockExact(5);


                if (!PlayerMineListener.armSwinging.containsKey(player.getName()) || currentBlock == null || !currentBlock.equals(originalBlock)) {
                    breakingAnimation.getIntegers().write(1, -1);
                    manager.sendServerPacket(player, breakingAnimation);
                    this.cancel();
                    return;
                }

                if (currentTicks >= breakingTimeTicks) {
                    breakingAnimation.getIntegers().write(1, 9);
                    manager.sendServerPacket(player, breakingAnimation);
                    playerBreakBlock(player, originalBlock);
                    breakingAnimation.getIntegers().write(1, -1);
                    manager.sendServerPacket(player, breakingAnimation);
                    this.cancel();
                    return;

                }

                double breakAmount = 1;

                if (!player.isOnGround()) {
                    breakAmount *= 0.5;
                }
                if (player.isInWater()) {
                    breakAmount *= 0.5;
                }

                Specialization.logger.info(String.valueOf(currentTicks));
                Specialization.logger.info(String.valueOf(breakAmount));

                currentTicks += breakAmount;

                breakingAnimation.getIntegers().write(1, (int) Math.floor(currentTicks/ticksPerState));
                manager.sendServerPacket(player, breakingAnimation);
            }
        }.runTaskTimer(Specialization.getInstance(), 0L, 1L);
    }

    public double getBreakingTime(Player player, Block block) {
//
//        ItemStack item = player.getEquipment().getItemInMainHand();
//
//        if (block.isPreferredTool(player.getEquipment().getItemInMainHand())) {
//            if (item.getType().name().matches("WOODEN_(PICKAXE|SHOVEL|AXE|HOE)")) speedMultiplier = 2d;
//
//            else if (item.getType().name().matches("STONE_(PICKAXE|SHOVEL|AXE|HOE)")) speedMultiplier = 4d;
//
//            else if (item.getType().name().matches("IRON_(PICKAXE|SHOVEL|AXE|HOE)")) speedMultiplier = 6d;
//
//            else if (item.getType().name().matches("DIAMOND_(PICKAXE|SHOVEL|AXE|HOE)")) speedMultiplier = 8d;
//
//            else if (item.getType().name().matches("NETHERITE_(PICKAXE|SHOVEL|AXE|HOE)")) speedMultiplier = 9d;
//
//            else if (item.getType().name().matches("GOLDEN_(PICKAXE|SHOVEL|AXE|HOE)")) speedMultiplier = 12d;
//
//            if (item.hasItemMeta()) {
//                if (item.getItemMeta().hasEnchant(Enchantment.EFFICIENCY)) {
//                    speedMultiplier += Math.pow(item.getEnchantmentLevel(Enchantment.EFFICIENCY), 2) + 1d;
//                }
//            }
//
//        }
//
//
//        if (player.hasPotionEffect(PotionEffectType.HASTE)) {
//            speedMultiplier *= 1 + (0.2 * player.getPotionEffect(PotionEffectType.HASTE).getAmplifier());
//        }
//
//        double damage;
//
//        damage = speedMultiplier / SpecializationConfig.getBlockHardnessConfig().get(block.getType(), Double.class);
//
//        damage /= 30;
//
//        // Instant breaking
//        if (damage > 1) {
//            return 0d;
//        }

        double multiplier = 1D;

        if (ReinforcementManager.isLightlyReinforced(block)) multiplier = SpecializationConfig.getBlockHardnessConfig().get("LIGHT_REINFORCEMENT_MULTIPLIER", Double.class);
        if (ReinforcementManager.isHeavilyReinforced(block)) multiplier = SpecializationConfig.getBlockHardnessConfig().get("HEAVY_REINFORCEMENT_MULTIPLIER", Double.class);

        return Math.round(SpecializationConfig.getBlockHardnessConfig().get(block.getType(), Double.class) * SpecializationConfig.getBlockHardnessConfig().get(block.getType(), Double.class) * multiplier);
    }

    public void playerBreakBlock(Player player, Block block) {
        CustomPlayer customPlayer = (CustomPlayer) MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().getCustomPlayer(player.getUniqueId());
        Specialization.logger.info(block.getType().name() + " ");
        Pair<SkillType, Double> pair = SpecializationConfig.getXpGainFromBreakingConfig().get(block.getType(), new TypeToken<>() {
        });
        customPlayer.addSkillXp(pair.firstValue(), pair.secondValue());

        ReinforcementManager.removeReinforcement(block);
        block.breakNaturally(player.getEquipment().getItemInMainHand(), true, true);
    }
}