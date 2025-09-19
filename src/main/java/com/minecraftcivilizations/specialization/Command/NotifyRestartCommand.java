package com.minecraftcivilizations.specialization.Command;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

@CommandAlias("notifyrestart")
@CommandPermission("civlabs.notifyrestart")
public class NotifyRestartCommand {

    @Default
    @CommandPermission("civlabs.notifyrestart")
    public void notifyRestart(@NotNull Integer min) {
        Bukkit.getOnlinePlayers().forEach(player -> {
            player.sendRichMessage("<red>This server will be shutting down in the next <white> " + min + "<red> min!");
        });
    }

}
