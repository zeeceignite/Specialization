package com.minecraftcivilizations.specialization.Command;

import com.minecraftcivilizations.specialization.Distance.Town;
import com.minecraftcivilizations.specialization.Distance.TownManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class TownsCommandExecutor implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("towns")) {
            if (!sender.hasPermission("towndetector.list")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }

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
            return true;
        }
        return false;
    }
}
