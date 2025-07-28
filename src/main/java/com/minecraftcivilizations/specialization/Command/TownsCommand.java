package com.minecraftcivilizations.specialization.Command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import com.minecraftcivilizations.specialization.Distance.Town;
import com.minecraftcivilizations.specialization.Distance.TownManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

@CommandAlias("towns")
public class TownsCommand extends BaseCommand {

    @Default
    @CommandPermission("towndetector.list")
    public void onTowns(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "=== Detected Towns ===");
        if (TownManager.getTowns().isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No towns detected yet.");
        } else {
            for (int i = 0; i < TownManager.getTowns().size(); i++) {
                Town town = TownManager.getTowns().get(i);
                sender.sendMessage(ChatColor.AQUA + "Town " + (i + 1) + ": " +
                        ChatColor.WHITE + town.getBedCount() + " beds at " +
                        TownManager.formatLocation(town.getCenterLocation()));
            }
        }
    }

}
