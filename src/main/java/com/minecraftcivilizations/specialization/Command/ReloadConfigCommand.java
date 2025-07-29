package com.minecraftcivilizations.specialization.Command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

@CommandAlias("reloadconfig")
public class ReloadConfigCommand extends BaseCommand {

    @Default
    public void onReloadConfig(@NotNull CommandSender sender) {
        if (sender.isOp()) {
            SpecializationConfig.reload();
        }
    }

}
