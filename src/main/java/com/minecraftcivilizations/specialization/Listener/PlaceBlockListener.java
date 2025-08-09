package com.minecraftcivilizations.specialization.Listener;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Reinforcement.ReinforcementManager;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.Specialization;
import minecraftcivilizations.com.minecraftCivilizationsCore.MinecraftCivilizationsCore;
import minecraftcivilizations.com.minecraftCivilizationsCore.Options.Pair;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

public class PlaceBlockListener implements Listener {
    private static final Logger LOGGER = Logger.getLogger(PlaceBlockListener.class.getName());

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();
        Material blockType = block.getType();
        
        Pair<SkillType, Double> pair = SpecializationConfig.getXpGainFromPlacingConfig().get(blockType, new TypeToken<>() {});
        if (pair != null) {
            CustomPlayer customPlayer = (CustomPlayer) MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().getCustomPlayer(player.getUniqueId());

            if (blockType.name().contains("BRICKS")) {
                applyBrickBreakSpeedModifier(player, block);
            }

            customPlayer.addSkillXp(pair.firstValue(), pair.secondValue());
        }
    }
    
    private void applyBrickBreakSpeedModifier(Player player, Block block) {

        double breakSpeedMultiplier = SpecializationConfig.getReinforcementConfig().get("HEAVY_REINFORCEMENT_MULTIPLIER", Double.class);
        String modifierName = "brick_break_speed";
        
        LOGGER.info("Applying brick break speed modifier (0.5x) to player " + player.getName() + 
                   " for block at " + block.getLocation());
        
        // Apply the attribute modifier to the player
        AttributeInstance blockBreakSpeed = player.getAttribute(Attribute.BLOCK_BREAK_SPEED);
        if (blockBreakSpeed != null) {
            // Remove any existing brick break speed modifiers
            blockBreakSpeed.getModifiers().stream()
                .filter(mod -> mod.getName().contains("brick_break_speed"))
                .forEach(blockBreakSpeed::removeModifier);
            
            // Create and add new modifier
            NamespacedKey modifierKey = new NamespacedKey(Specialization.getInstance(), modifierName);
            AttributeModifier modifier = new AttributeModifier(
                modifierKey,
                breakSpeedMultiplier - 1.0, // Subtract 1 because MULTIPLY_SCALAR_1 adds to base value
                AttributeModifier.Operation.MULTIPLY_SCALAR_1,
                EquipmentSlotGroup.ANY
            );
            
            blockBreakSpeed.addModifier(modifier);
            LOGGER.info("Added " + modifierName + " modifier (" + breakSpeedMultiplier + "x) to player " + player.getName());
        } else {
            LOGGER.warning("Could not get BLOCK_BREAK_SPEED attribute for player " + player.getName());
        }
    }
}
