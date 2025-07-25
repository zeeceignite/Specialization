package com.minecraftcivilizations.specialization.Listener;

import com.google.gson.Gson;
import com.minecraftcivilizations.specialization.Config.Config;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Recipe.Pair;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import minecraftcivilizations.com.minecraftCivilizationsCore.MinecraftCivilizationsCore;
import org.bukkit.Material;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.ItemStack;

public class FurnaceListener implements Listener {
    @EventHandler(ignoreCancelled = true)
    public void onSmelt(FurnaceExtractEvent event) {
        Player player = event.getPlayer();
        Material extracted = event.getItemType();
        int amount = event.getItemAmount();

        if (event.getBlock().getType() == Material.FURNACE) {
            furnaceSmelt(player, new ItemStack(extracted, amount), amount);
        } else if (event.getBlock().getType() == Material.FURNACE_MINECART) {
            furnaceSmelt(player, new ItemStack(extracted, amount), amount);
        } else if (event.getBlock().getType() == Material.SMOKER) {
            smokerSmelt(player, new ItemStack(extracted, amount), amount);
        } else if (event.getBlock().getType() == Material.BLAST_FURNACE) {
            blastSmelt(player, new ItemStack(extracted, amount), amount);
        }

    }

    private void furnaceSmelt(Player player, ItemStack item, int amount) {
        CustomPlayer customPlayer = (CustomPlayer) MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().getCustomPlayer(player.getUniqueId());
        Pair pair = new Gson().fromJson(Config.getXpGainFromSmeltingConfig().getString(item.getType().name()), Pair.class);
        customPlayer.addSkillXp(SkillType.valueOf(pair.key()), Double.parseDouble(pair.value()) * amount);
    }

    private void smokerSmelt(Player player, ItemStack item, int amount) {
        CustomPlayer customPlayer = (CustomPlayer) MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().getCustomPlayer(player.getUniqueId());
        Pair pair = new Gson().fromJson(Config.getXpGainFromSmokingConfig().getString(item.getType().name()), Pair.class);
        customPlayer.addSkillXp(SkillType.valueOf(pair.key()), Double.parseDouble(pair.value()) * amount);
    }

    private void blastSmelt(Player player, ItemStack item, int amount) {
        CustomPlayer customPlayer = (CustomPlayer) MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().getCustomPlayer(player.getUniqueId());
        Pair pair = new Gson().fromJson(Config.getXpGainFromBlastingConfig().getString(item.getType().name()), Pair.class);
        customPlayer.addSkillXp(SkillType.valueOf(pair.key()), Double.parseDouble(pair.value()) * amount);
    }
}
