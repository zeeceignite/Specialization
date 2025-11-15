package com.minecraftcivilizations.specialization.Command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Specialization;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

@CommandAlias("giveup|suicide|die")
public class SuicideCommand extends BaseCommand {

    @Default
    public void onSuicide(@NotNull Player player) {
        CustomPlayer customPlayer = CoreUtil.getPlayer(player);

        Byte downed = player.getPersistentDataContainer().get(new NamespacedKey(Specialization.getInstance(), "is_downed"), PersistentDataType.BYTE);
        if (downed == null || downed == 0) {
            player.sendMessage(Component.text("You can only use this command while downed." + downed).color(NamedTextColor.RED));
            return;
        }
        player.setHealth(0);

        customPlayer.setDowned(false);
    }


    // for testing
    @CommandAlias("revive")
    @CommandPermission("civlabs.selfrevive")
    public void revive(@NotNull Player player) {

        NamespacedKey key = new NamespacedKey(Specialization.getInstance(), "is_downed");
        player.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 0);

        player.sendMessage(Component.text("You revived yourself.").color(NamedTextColor.GREEN));
    }
}
