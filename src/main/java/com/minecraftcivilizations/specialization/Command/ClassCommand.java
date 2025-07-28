package com.minecraftcivilizations.specialization.Command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import com.minecraftcivilizations.specialization.GUI.MainMenuGUI;
import com.minecraftcivilizations.specialization.Specialization;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@CommandAlias("class")
public class ClassCommand extends BaseCommand {

    @Default
    public void onClass(@NotNull CommandSender sender) {
        if (sender instanceof Player player && player.isOp()) {
            Specialization.logger.info("Opening GUI for " + player.getName());
            new MainMenuGUI("Your Specialization Stats").open(player);
            Specialization.logger.info("Opened GUI for " + player.getName());
        }
    }

}