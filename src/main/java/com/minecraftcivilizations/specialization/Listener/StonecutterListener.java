package com.minecraftcivilizations.specialization.Listener;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.Specialization;
import minecraftcivilizations.com.minecraftCivilizationsCore.MinecraftCivilizationsCore;
import minecraftcivilizations.com.minecraftCivilizationsCore.Options.Pair;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

public class StonecutterListener implements Listener {
    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(InventoryClickEvent event) {
        if (event.getView().getType() != InventoryType.STONECUTTER) return;

        if (event.getSlot() != 1) return;

        if (!(event.getWhoClicked() instanceof Player player)) return;

        int amount = 1;

        if (!event.getAction().toString().startsWith("PICKUP")) {
            if (event.getAction().equals(InventoryAction.MOVE_TO_OTHER_INVENTORY)) {
                amount = event.getCurrentItem().getAmount() * event.getView().getItem(0).getAmount() == 0 ? 1 : event.getCurrentItem().getAmount() * event.getView().getItem(0).getAmount();
            }
        }

        ItemStack result = event.getCurrentItem();
        if (result == null || result.getType() == Material.AIR) return;

        Specialization.logger.info(String.valueOf(event.getView().getItem(event.getSlot()).getAmount()));

        CustomPlayer customPlayer = (CustomPlayer) MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().getCustomPlayer(player.getUniqueId());
        Pair<SkillType, Double> pair = SpecializationConfig.getXpGainFromStonecuttingConfig().get(result.getType(), new TypeToken<>(){});
        customPlayer.addSkillXp(pair.firstValue(), pair.secondValue() * amount);
    }
}
