package com.minecraftcivilizations.specialization.Debug;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.PaperCommandManager;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.ArrayList;

/**
 * Homebrewed Debug System. Use it.
 * See DebugUtils
 * @author alectriciti
 */
@CommandAlias("debug|debuglisten")
public class DebugListenCommand extends BaseCommand implements Listener {


    public DebugListenCommand(PaperCommandManager commandManager) {
        commandManager.registerCommand(this);
        registerCompletions(commandManager);
    }

    @Subcommand("add")
    @CommandCompletion("@debug_channels_add")
    public void onAdd(Player player, @Optional String debug_channel) {
        if (debug_channel == null || debug_channel.isBlank()) {
            player.sendMessage(ChatColor.RED + "Usage: /debug add <debug_channel>");
            return;
        }
        Debug.getInstance().registerPlayerChannel(player, debug_channel);
        player.sendMessage(ChatColor.GREEN + "Registered to debug channel: " + ChatColor.YELLOW + debug_channel);
    }

    @Subcommand("remove")
    @CommandCompletion("@debug_channels_remove")
    public void onRemove(Player player, @Optional String debug_channel) {
        if (debug_channel == null || debug_channel.isBlank()) {
            player.sendMessage(ChatColor.RED + "Usage: /debug remove <debug_channel>");
            return;
        }
        Debug.getInstance().unregisterPlayerChannel(player, debug_channel);
        player.sendMessage(ChatColor.GREEN + "Unregistered from debug channel: " + ChatColor.YELLOW + debug_channel);
    }

    public static void registerCompletions(PaperCommandManager manager) {
        if (manager == null) return;
        manager.getCommandCompletions().registerCompletion("debug_channels_add", c -> {
            try {
                return Debug.getChannelList();
            } catch (Exception e) {
                return new ArrayList<String>();
            }
        });
        manager.getCommandCompletions().registerCompletion("debug_channels_remove", c -> {
            try {
                return Debug.getPlayerChannels(c.getPlayer());
            } catch (Exception e) {
                return new ArrayList<String>();
            }
        });
    }
}
