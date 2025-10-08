package com.minecraftcivilizations.specialization.Command;

import com.minecraftcivilizations.specialization.Player.LocalNameGenerator;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Specialization;
import minecraftcivilizations.com.minecraftCivilizationsCore.MinecraftCivilizationsCore;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.NoSuchElementException;

@CommandAlias("rerollname|reroll")
@CommandPermission("specialization.rerollname")
public class RerollNameCommand extends BaseCommand {

    private final LocalNameGenerator nameGenerator;

    public RerollNameCommand(LocalNameGenerator generator) {
        this.nameGenerator = generator;
    }

    @Default
    @CommandPermission("specialization.rerollname")
    @Description("Reroll the username of a player")
    @Syntax("<player>")
    public void rerollName(Player sender, @NotNull String targetName) {
        Player target = Bukkit.getPlayerExact(targetName);

        if (target == null || !target.isOnline()) {
            sender.sendMessage("§cPlayer not found or not online.");
            return;
        }

        try {
            String newNameStr = nameGenerator.nextName();
            Component newName = Component.text(newNameStr)
                    .color(NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false);

            // Update the CustomPlayer object
            CustomPlayer customPlayer = (CustomPlayer) MinecraftCivilizationsCore
                    .getInstance()
                    .getCustomPlayerManager()
                    .getCustomPlayer(target.getUniqueId());

            customPlayer.setName(newName);

            // Apply name to player and all online players
            Specialization.getInstance().applyCustomName(target, newName);

            sender.sendMessage("§aSuccessfully rerolled " + target.getName() + " to " + newNameStr + ".");
            target.sendMessage("§eYour username has been rerolled to " + newNameStr + "!");

        } catch (NoSuchElementException e) {
            sender.sendMessage("§cCould not generate a new unique name. Try again later.");
        }
    }
}
