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
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import java.util.UUID;

@CommandAlias("setnameoption")
//No permissions needed here. Handles all exceptions afaik.
//@CommandPermission("specialization.setnameoption")
public class NameChoiceCommand extends BaseCommand {

    private final LocalNameGenerator nameGenerator;

    public NameChoiceCommand(LocalNameGenerator generator) {
        this.nameGenerator = generator;
    }
    @Subcommand("confirm")
    @Description("Confirm a temporary name choice")
    @Syntax("/setnameoption confirm <chosenName>")
    public void confirm(Player sender, String chosenName) {
        if (chosenName == null || chosenName.isEmpty()) {
            sender.sendMessage("§cYou must specify a name to confirm.");
            return;
        }

        // Use the helper from LocalNameGenerator
        if (!nameGenerator.canSelectTempName(sender)) {
            sender.sendMessage("§cYou can no longer confirm a temporary name because you have played longer than 10 minutes");
            return;
        }

        UUID uuid = sender.getUniqueId();

        boolean confirmed = nameGenerator.confirmNameChoice(uuid, chosenName);
        if (!confirmed) {
            sender.sendMessage("§cInvalid choice or you have already chosen a name");
            return;
        }

        // Update CustomPlayer object
        CustomPlayer customPlayer = (CustomPlayer) MinecraftCivilizationsCore
                .getInstance()
                .getCustomPlayerManager()
                .getCustomPlayer(uuid);

        Component newName = Component.text(chosenName)
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false);

        customPlayer.setName(newName);

        Player bukkitPlayer = Bukkit.getPlayer(uuid);
        if (bukkitPlayer != null) {
            Specialization.getInstance().applyCustomName(bukkitPlayer, newName);
            // simulate writing and sealing a name
            sender.playSound(sender.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.UI,  0.8f, 1.2f);
            sender.playSound(sender.getLocation(), Sound.ITEM_BOOK_PUT, SoundCategory.UI,  0.5f, 1f);
            bukkitPlayer.sendMessage(
                    Component.text("Your name is now: ", NamedTextColor.GREEN)
                            .append(newName.color(NamedTextColor.GOLD))
            );
        }
    }


    @Default
    @Description("Choose one of your temporary name options")
    @Syntax("/setnameoption <tempName>")
    public void choose(Player sender, String tempName) {
        if (tempName == null || tempName.isEmpty()) {
            sender.sendMessage("§cYou must specify a temporary name to select.");
            return;
        }

        // Suggest the confirm command as a clickable chat component
        Component confirmPopup = Component.text(" [Confirm & Apply]", NamedTextColor.GREEN)
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                        Component.text("Permanently set name to: " + tempName)))
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand(
                        "/setnameoption confirm " + tempName));

        sender.sendMessage(
                Component.text("Are you sure? ", NamedTextColor.WHITE)
                        .append(Component.text( tempName, NamedTextColor.GOLD))
                        .append(Component.text(" →", NamedTextColor.AQUA))
                        .append(confirmPopup)
        );
    }
}
