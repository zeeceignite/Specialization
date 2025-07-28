package com.minecraftcivilizations.specialization.Command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import minecraftcivilizations.com.minecraftCivilizationsCore.MinecraftCivilizationsCore;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@CommandAlias("setxp")
public class SetXpCommand extends BaseCommand {

    @Default
    public void onSetXP(@NotNull Player player, @NotNull SkillType type, double amount) {
        if (player.isOp()) {
            CustomPlayer customPlayer = (CustomPlayer) MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().getCustomPlayer(player.getUniqueId());
            customPlayer.addSkillXp(type, amount);
        }
    }

}
