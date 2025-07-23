package com.minecraftcivilizations.specialization.Command;

import com.minecraftcivilizations.specialization.GUI.MainMenuGUI;
import com.minecraftcivilizations.specialization.Specialization;
import minecraftcivilizations.com.minecraftCivilizationsCore.GUI.ListGUI;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ClassCommandExecutor implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (sender instanceof Player player && player.isOp()) {
            Specialization.logger.info("Opening GUI for " + player.getName());
            new MainMenuGUI("Your Specialization Stats").open(player);
            Specialization.logger.info("Opened GUI for " + player.getName());
        }
        return true;
    }
}
