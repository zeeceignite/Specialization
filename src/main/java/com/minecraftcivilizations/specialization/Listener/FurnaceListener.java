package com.minecraftcivilizations.specialization.Listener;

import com.google.gson.Gson;
import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Recipe.Pair;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import minecraftcivilizations.com.minecraftCivilizationsCore.Config.Config;
import minecraftcivilizations.com.minecraftCivilizationsCore.MinecraftCivilizationsCore;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.inventory.ItemStack;

public class FurnaceListener implements Listener {
    @EventHandler(ignoreCancelled = true)
    public void onSmelt(FurnaceExtractEvent event) {
        Player player = event.getPlayer();
        Material extracted = event.getItemType();
        int amount = event.getItemAmount();

        if (event.getBlock().getType() == Material.FURNACE) {
            smelt(player, new ItemStack(extracted, amount), amount, SpecializationConfig.getXpGainFromSmeltingConfig());
        } else if (event.getBlock().getType() == Material.SMOKER) {
            smelt(player, new ItemStack(extracted, amount), amount, SpecializationConfig.getXpGainFromSmokingConfig());
        } else if (event.getBlock().getType() == Material.BLAST_FURNACE) {
            smelt(player, new ItemStack(extracted, amount), amount, SpecializationConfig.getXpGainFromSmokingConfig());
        }

    }

    private void smelt(Player player, ItemStack item, int amount,  Config xpGain){
        CustomPlayer customPlayer = (CustomPlayer) MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().getCustomPlayer(player.getUniqueId());
        Pair pair = new Gson().fromJson(xpGain.getString(item.getType().name()), Pair.class);
        customPlayer.addSkillXp(SkillType.valueOf(pair.key()), Double.parseDouble(pair.value()) * amount);
    }
}
