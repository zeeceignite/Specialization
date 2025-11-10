package com.minecraftcivilizations.specialization.Listener.Player.Blocks.Mining;

import com.google.gson.reflect.TypeToken;
import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillLevel;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.Specialization;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import minecraftcivilizations.com.minecraftCivilizationsCore.Options.Pair;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Door;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static com.minecraftcivilizations.specialization.Reinforcement.ReinforcementManager.*;

public class BreakBlockListener implements Listener {

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        AttributeInstance breakSpeedAttr = event.getPlayer().getAttribute(Attribute.BLOCK_BREAK_SPEED);

        if (breakSpeedAttr != null) {
            breakSpeedAttr.setBaseValue(SpecializationConfig.getBlockHardnessConfig().get(event.getBlock().getType(), Double.class));
            Pair<SkillType, Double> pair = SpecializationConfig.getXpGainFromBreakingConfig().get(event.getBlock().getType(), new TypeToken<>() {});
            CustomPlayer player = CoreUtil.getPlayer(event.getPlayer().getUniqueId());
            BlockData blockData = event.getBlock().getBlockData();

            if (isReinforced(event.getBlock())) {
                handleReinforcedDrop(event.getBlock(), event.getPlayer());
            }

            if (pair != null && pair.firstValue() != null && pair.secondValue() != null) {
                if (blockData instanceof Ageable age) {
                    if (age.getMaximumAge() == age.getAge()) {
                        player.addSkillXp(pair.firstValue(), pair.secondValue(), event.getBlock().getLocation());
                    }
                } else {
                    player.addSkillXp(pair.firstValue(), pair.secondValue(), event.getBlock().getLocation());
                }
            }
        }
        minerListener(event);
        farmerListener(event);
    }

    private void handleReinforcedDrop(Block block, org.bukkit.entity.Player player) {
        Location dropLocation = block.getLocation().add(0.5, 0.5, 0.5);

        if (isHeavilyReinforced(block)) {
            block.getWorld().dropItemNaturally(dropLocation, new ItemStack(Material.IRON_INGOT));
            player.sendMessage("You have received 1 iron ingot for breaking heavily reinforced blocks!");
        }
        if (isLightlyReinforced(block)) {
            block.getWorld().dropItemNaturally(dropLocation, new ItemStack(Material.COPPER_INGOT));
            player.sendMessage("You have received 1 copper ingot for breaking lightly reinforced blocks!");
        }
        for (Block b : getMultiBlocks(block)) {
            removeReinforcement(b);
        }
    }

    public void minerListener(BlockBreakEvent event) {
        CustomPlayer player = CoreUtil.getPlayer(event.getPlayer());
        Material materialName = event.getBlock().getType();
        SkillLevel skillRequired = SpecializationConfig.getCanMinerLvlBreakConfig().get(materialName.toString(), new TypeToken<>() {});
        if (skillRequired != null && player.getSkillLevel(SkillType.MINER) < skillRequired.getLevel()) {
            event.setDropItems(false);
            if(event.getPlayer().getGameMode() == GameMode.SURVIVAL)
            event.getPlayer().sendMessage("You are unable to mine this ore.");
        }
    }

    public void farmerListener(BlockBreakEvent event) {
        CustomPlayer player = CoreUtil.getPlayer(event.getPlayer());
        Material materialName = event.getBlock().getType();
        SkillLevel skillRequired = SpecializationConfig.getCanFarmerBreakConfig().get(materialName.toString(), new TypeToken<>() {});

        if (skillRequired != null && player.getSkillLevel(SkillType.FARMER) < skillRequired.getLevel()) {
            event.setDropItems(false);
            event.getPlayer().sendMessage(org.bukkit.ChatColor.RED + "You are unable to farm this");
        }

        List<Material> otherFarmables = List.of(Material.COCOA_BEANS, Material.SUGAR_CANE, Material.CACTUS, Material.MELON, Material.PUMPKIN);
        double chance = SpecializationConfig.getFarmerConfig().get("FARMER_GET_DROPS_CHANCE_" + player.getSkillLevelEnum(SkillType.FARMER), Double.class);
        double random = Math.random();

        if(random < chance) {
            event.setDropItems(true);
        } else if (event.getBlock().getBlockData() instanceof Ageable || otherFarmables.contains(materialName)) {
            event.setDropItems(false);
        }
    }

    private List<Block> getMultiBlocks(Block b) {
        List<Block> l = new ArrayList<>();
        l.add(b);
        BlockData d = b.getBlockData();
        switch (d) {
            case Door door -> l.add(b.getRelative(door.getHalf() == Bisected.Half.TOP ? BlockFace.DOWN : BlockFace.UP));
            case Bed bed -> l.add(b.getRelative(bed.getPart() == Bed.Part.HEAD ? bed.getFacing().getOppositeFace() : bed.getFacing()));
            case Bisected bi -> l.add(b.getRelative(bi.getHalf() == Bisected.Half.TOP ? BlockFace.DOWN : BlockFace.UP));
            default -> {}
        }
        return l;
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    private void handleExplosion(List<Block> blocks) {
        blocks.forEach(block -> {
            if (!isReinforced(block)) return;

            Location dropLocation = block.getLocation().add(0.5, 0.5, 0.5);
            Material type = block.getType();
            org.bukkit.block.data.BlockData data = block.getBlockData();

            if (isHeavilyReinforced(block)) {
                double heavy = SpecializationConfig.getReinforcementConfig().get("HEAVY_EXPLOSION_RESISTANCE", Double.class);
                Bukkit.getScheduler().runTaskLater(Specialization.getInstance(), () -> {
                    if (Math.random() < heavy) {
                        block.setType(type);
                        block.setBlockData(data);
                    } else {
                        block.getWorld().dropItemNaturally(dropLocation, new ItemStack(Material.IRON_INGOT));
                    }
                }, 3L);
            }

            if (isLightlyReinforced(block)) {
                double light = SpecializationConfig.getReinforcementConfig().get("LIGHT_EXPLOSION_RESISTANCE", Double.class);
                Bukkit.getScheduler().runTaskLater(Specialization.getInstance(), () -> {
                    if (Math.random() < light) {
                        block.setType(type);
                        block.setBlockData(data);
                    } else {
                        block.getWorld().dropItemNaturally(dropLocation, new ItemStack(Material.COPPER_INGOT));
                    }
                }, 3L);
            }

            for (Block b : getMultiBlocks(block)) removeReinforcement(b);
        });
    }
}
