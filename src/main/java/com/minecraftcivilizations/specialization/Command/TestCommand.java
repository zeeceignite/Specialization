package com.minecraftcivilizations.specialization.Command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import com.minecraftcivilizations.specialization.Listener.Player.PlayerDeathListener;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import minecraftcivilizations.com.minecraftCivilizationsCore.GUI.GUIs.ScrollableHorizontalGUI;
import minecraftcivilizations.com.minecraftCivilizationsCore.GUI.GUIs.ScrollableVerticalGUI;
import minecraftcivilizations.com.minecraftCivilizationsCore.GUI.GUIs.SearchSignGUI;
import minecraftcivilizations.com.minecraftCivilizationsCore.Util.SearchUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import javax.swing.plaf.basic.BasicSplitPaneUI;

@CommandAlias("test")
public class TestCommand extends BaseCommand {

    @Default
    public void onRun(@NotNull Player player) {
        player.sendMessage(String.valueOf(SearchSignGUI.searchItemsAndBlocks("a").size()));
        new ScrollableHorizontalGUI(Component.text("test"), 54, SearchSignGUI.searchItemsAndBlocks("a")).open(player);
    }
}
