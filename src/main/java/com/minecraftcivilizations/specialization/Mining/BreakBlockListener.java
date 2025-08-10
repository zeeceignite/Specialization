package com.minecraftcivilizations.specialization.Mining;

import com.google.gson.reflect.TypeToken;
import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillLevel;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import minecraftcivilizations.com.minecraftCivilizationsCore.Options.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Ageable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;

import static com.minecraftcivilizations.specialization.Reinforcement.ReinforcementManager.*;

public class BreakBlockListener implements Listener {

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        AttributeInstance breakSpeedAttr = event.getPlayer().getAttribute(Attribute.BLOCK_BREAK_SPEED);

        if (breakSpeedAttr != null) {
            breakSpeedAttr.setBaseValue(SpecializationConfig.getBlockHardnessConfig().get(event.getBlock().getType(), Double.class));
            Pair<SkillType, Double> pair = SpecializationConfig.getXpGainFromBreakingConfig().get(event.getBlock().getType(), new TypeToken<>() {});
            CustomPlayer player = CoreUtil.getPlayer(event.getPlayer().getUniqueId());
            BlockData blockData = event.getBlock().getType().createBlockData();

            if (blockData instanceof Ageable age) {
                if (age.getAge() < 7) {
                    player.addSkillXp(pair.firstValue(), 0);
                    System.out.println("I broke a baby plant or something");
                }
            }

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
            if (pair != null && pair.firstValue() != null && pair.secondValue() != null) {
                player.addSkillXp(pair.firstValue(), pair.secondValue());
            }
        }
        minerListener(event);
    }

    public void minerListener(BlockBreakEvent event) {
        CustomPlayer player = CoreUtil.getPlayer(event.getPlayer());
        Material materialName = event.getBlock().getType();
        SkillLevel skillRequired = SpecializationConfig.getCanMinerLvlBreakConfig().get(materialName.toString(), new TypeToken<SkillLevel>() {});
        if (skillRequired != null && player.getSkillLevel(SkillType.MINER) < skillRequired.getLevel()) {
            event.setDropItems(false);
            event.getPlayer().sendMessage(org.bukkit.ChatColor.RED + "You are unable to mine this ore.");
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
    public void farmerListener(BlockBreakEvent event) {
        CustomPlayer player = CoreUtil.getPlayer(event.getPlayer());
        Material materialName = event.getBlock().getType();
        SkillLevel skillRequired = SpecializationConfig.getCanMinerLvlBreakConfig().get(materialName.toString(), new TypeToken<>() {});
        if (skillRequired != null && player.getSkillLevel(SkillType.FARMER) < skillRequired.getLevel()) {
            event.setDropItems(false);
            event.getPlayer().sendMessage(org.bukkit.ChatColor.RED + "You are unable to farm this");
        }
    }

}