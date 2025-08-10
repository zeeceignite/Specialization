package com.minecraftcivilizations.specialization.Command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import com.minecraftcivilizations.specialization.Listener.PlayerDeathListener;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@CommandAlias("suicide")
public class SuicideCommand extends BaseCommand {

    @Default
    public void onSuicide(@NotNull Player player) {
        CustomPlayer customPlayer = CoreUtil.getPlayer(player.getUniqueId());
        
        if (customPlayer == null) {
            player.sendMessage(Component.text("Error: Could not find player data.").color(NamedTextColor.RED));
            return;
        }

        if (!customPlayer.isDowned()) {
            player.sendMessage(Component.text("You can only use this command while downed.").color(NamedTextColor.RED));
            return;
        }

        PlayerDeathListener playerDeathListener = new PlayerDeathListener();
        PlayerDeathListener.removeDownedArmorStand(player);
        playerDeathListener.playerActuallyDied(player);
        player.setHealth(0);

        customPlayer.setDowned(false);
    }
}
