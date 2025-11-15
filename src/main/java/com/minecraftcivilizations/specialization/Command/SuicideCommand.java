package com.minecraftcivilizations.specialization.Command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import com.minecraftcivilizations.specialization.Listener.Player.PlayerDownedListener;
import com.minecraftcivilizations.specialization.Specialization;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

@CommandAlias("giveup|suicide|die")
public class SuicideCommand extends BaseCommand {
    private final PlayerDownedListener downedListener;

    public SuicideCommand(PlayerDownedListener downedListener) {
        this.downedListener = downedListener;
        // Registering the command class elsewhere via ACF
    }

    @Default
    public void onSuicide(@NotNull Player player) {

        Byte downed = player.getPersistentDataContainer().get(new NamespacedKey(Specialization.getInstance(), "is_downed"), PersistentDataType.BYTE);
        if (downed == null || downed == 0) {
            player.sendMessage(Component.text("You can only use this command while downed.").color(NamedTextColor.RED));
            return;
        }
        player.setHealth(0);
    }



    // -------------------------
    // /down — manually down yourself
    // -------------------------
    @CommandAlias("down|downed")
    @CommandPermission("civlabs.selfdown")
    public void downSelf(@NotNull Player player) {
        if (downedListener.isDowned(player)) {
            player.sendMessage(Component.text("You are already downed.").color(NamedTextColor.RED));
            return;
        }

        // Down the player using PlayerDownedListener
        downedListener.setDowned(player, true, 10); // 10 = default health when downed
        player.sendMessage(Component.text("You are now downed.").color(NamedTextColor.YELLOW));
    }

    // -------------------------
    // /up — manually revive yourself
    // -------------------------
    @CommandAlias("revive|getup|selfrevive")
    @CommandPermission("civlabs.selfrevive")
    public void upSelf(@NotNull Player player) {
        if (!downedListener.isDowned(player)) {
            player.sendMessage(Component.text("You are not downed.").color(NamedTextColor.RED));
            return;
        }

        downedListener.setDowned(player, false, player.getHealth());
        player.sendMessage(Component.text("You are no longer downed.").color(NamedTextColor.GREEN));
    }
}

