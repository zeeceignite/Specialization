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
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;

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

            if(isReinforced(event.getBlock())) {
                Location dropLocation = event.getBlock().getLocation().add(0.5, 0.5, 0.5);

                if(isHeavilyReinforced(event.getBlock())) {
                    event.getBlock().getWorld().dropItemNaturally(dropLocation, new ItemStack(Material.IRON_INGOT, 1));
                    event.getPlayer().sendMessage("You have received 1 iron ingot for breaking heavily reinforced blocks!");
                }
                if(isLightlyReinforced(event.getBlock())) {
                    event.getBlock().getWorld().dropItemNaturally(dropLocation, new ItemStack(Material.COPPER_INGOT, 1));
                    event.getPlayer().sendMessage("You have received 1 iron nugget for breaking lightly reinforced blocks!");
                }
                removeReinforcement(event.getBlock());
            }
            if (pair != null && pair.firstValue() != null && pair.secondValue() != null) {
                if (blockData instanceof Ageable age && age.getMaximumAge() == age.getAge()) {
                    player.addSkillXp(pair.firstValue(), 0);
                }else {
                    player.addSkillXp(pair.firstValue(), pair.secondValue());
                }
            }
        }
        minerListener(event);
    }

    public void minerListener(BlockBreakEvent event) {
        CustomPlayer player = CoreUtil.getPlayer(event.getPlayer());
        Material materialName = event.getBlock().getType();
        SkillLevel skillRequired = SpecializationConfig.getCanMinerLvlBreakConfig().get(materialName.toString(), new TypeToken<>() {});
        if (skillRequired != null && player.getSkillLevel(SkillType.MINER) < skillRequired.getLevel()) {
            event.setDropItems(false);
            event.getPlayer().sendMessage(org.bukkit.ChatColor.RED + "You are unable to mine this ore.");
        }
    }


    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        onBlocksExplode(event.blockList());
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        onBlocksExplode(event.blockList());
    }

    private void onBlocksExplode(List<Block> blocks) {
        blocks.forEach(block -> {
            if(isReinforced(block)) {
                Location dropLocation = block.getLocation().add(0.5, 0.5, 0.5);

                if(isHeavilyReinforced(block)) {
                    double heavy = SpecializationConfig.getReinforcementConfig().get("HEAVY_EXPLOSION_RESISTANCE", Double.class);
                    Material type = block.getType();
                    BlockData data = block.getBlockData();
                    Bukkit.getScheduler().runTaskLater(Specialization.getInstance(),() -> {
                        if(Math.random() < heavy) {
                            block.setType(type);
                            block.setBlockData(data);
                        }else block.getWorld().dropItemNaturally(dropLocation, new ItemStack(Material.IRON_INGOT, 1));
                    }, 3);
                }
                if(isLightlyReinforced(block)) {
                    double light = SpecializationConfig.getReinforcementConfig().get("LIGHT_EXPLOSION_RESISTANCE", Double.class);
                    Material type = block.getType();
                    BlockData data = block.getBlockData();
                    Bukkit.getScheduler().runTaskLater(Specialization.getInstance(),() -> {
                        if(Math.random() < light) {
                            block.setType(type);
                            block.setBlockData(data);
                        }else block.getWorld().dropItemNaturally(dropLocation, new ItemStack(Material.COPPER_INGOT, 1));
                    },3);
                }
                removeReinforcement(block);
            }
        });
    }

    @EventHandler
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
        }else {
            if (event.getBlock().getBlockData() instanceof Ageable || otherFarmables.contains(materialName)) {
                event.setDropItems(false);
            }
        }
    }

}