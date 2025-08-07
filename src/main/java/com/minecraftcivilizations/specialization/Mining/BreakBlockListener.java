package com.minecraftcivilizations.specialization.Mining;

import com.google.gson.reflect.TypeToken;
import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import minecraftcivilizations.com.minecraftCivilizationsCore.Options.Pair;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.inventory.ItemStack;

import static com.minecraftcivilizations.specialization.Reinforcement.ReinforcementManager.*;

public class BreakBlockListener implements Listener {

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        AttributeInstance breakSpeedAttr = event.getPlayer().getAttribute(Attribute.BLOCK_BREAK_SPEED);
        if (breakSpeedAttr != null) {
            breakSpeedAttr.setBaseValue(1.0);
            Pair<SkillType, Double> pair = SpecializationConfig.getXpGainFromPlacingConfig().get(event.getBlock().getType(), new TypeToken<>() {});
            CustomPlayer player = CoreUtil.getPlayer(event.getPlayer());

            if(isReinforced(event.getBlock())) {
                Location dropLocation = event.getBlock().getLocation().add(0.5, 0.5, 0.5);

                if(isHeavilyReinforced(event.getBlock())) {
                    event.getBlock().getWorld().dropItemNaturally(dropLocation, new ItemStack(Material.IRON_INGOT, 1));
                    event.getPlayer().sendMessage("You have received 1 iron ingot for breaking heavily reinforced blocks!");
                }
                if(isLightlyReinforced(event.getBlock())) {
                    event.getBlock().getWorld().dropItemNaturally(dropLocation, new ItemStack(Material.IRON_NUGGET, 1));
                    event.getPlayer().sendMessage("You have received 1 iron nugget for breaking lightly reinforced blocks!");
                }
                removeReinforcement(event.getBlock());
            }
            player.addSkillXp(pair.firstValue(), pair.secondValue());
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().forEach(block -> {
            if(isReinforced(block)) {
                Location dropLocation = block.getLocation().add(0.5, 0.5, 0.5);

                if(isHeavilyReinforced(block)) {
                    block.getWorld().dropItemNaturally(dropLocation, new ItemStack(Material.IRON_INGOT, 1));
                }
                if(isLightlyReinforced(block)) {
                    block.getWorld().dropItemNaturally(dropLocation, new ItemStack(Material.IRON_NUGGET, 1));
                }
                removeReinforcement(block);
            }
        });
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().forEach(block -> {
            if(isReinforced(block)) {
                Location dropLocation = block.getLocation().add(0.5, 0.5, 0.5);

                if(isHeavilyReinforced(block)) {
                    block.getWorld().dropItemNaturally(dropLocation, new ItemStack(Material.IRON_INGOT, 1));
                }
                if(isLightlyReinforced(block)) {
                    block.getWorld().dropItemNaturally(dropLocation, new ItemStack(Material.IRON_NUGGET, 1));
                }
                removeReinforcement(block);
            }
        });
    }

    @EventHandler
    public void minerListener(BlockBreakEvent event) {
        CustomPlayer player =  CoreUtil.getPlayer(event.getPlayer());
        String materialName = event.getBlock().getType().name();
        Integer skillRequired = SpecializationConfig.getCanMinerLvlBreakConfig().get(materialName, new TypeToken<>() {});
        if (skillRequired != null && player.getSkillLevel(SkillType.MINER) < skillRequired) {
            event.setDropItems(false);
            event.getPlayer().sendMessage(Color.RED + "You are unable to mine this ore.");
        }
    }

    @EventHandler
    public void farmerListener(BlockBreakEvent event) {
        CustomPlayer player =  CoreUtil.getPlayer(event.getPlayer());
        String materialName = event.getBlock().getType().name();
        Integer skillRequired = SpecializationConfig.getCanFarmerBreakConfig().get(materialName, new TypeToken<>() {});
        if (skillRequired != null && player.getSkillLevel(SkillType.FARMER) < skillRequired) {
            event.setDropItems(false);
            event.getPlayer().sendMessage(Color.RED + "You are unable to farm this.");
        }
    }
}