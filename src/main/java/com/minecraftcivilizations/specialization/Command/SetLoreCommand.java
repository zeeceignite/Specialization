package com.minecraftcivilizations.specialization.Command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import com.minecraftcivilizations.specialization.Specialization;
import minecraftcivilizations.com.minecraftCivilizationsCore.Item.CustomItem;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

@CommandAlias("setlore")
public class SetLoreCommand extends BaseCommand {

    @Default
    public void onSetLore(@NotNull CommandSender sender, String lore) {
        if (sender instanceof Player player) {
            CustomItem.addLore(
                    player.getInventory().getItemInMainHand(), new ArrayList<>() {
                        {
                            add(Component.text(lore));
                        }
                    },
                    Specialization.getInstance()
            );
        }
    }

}
