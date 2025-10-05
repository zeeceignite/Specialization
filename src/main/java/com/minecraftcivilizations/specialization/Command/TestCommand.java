package com.minecraftcivilizations.specialization.Command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import com.minecraftcivilizations.specialization.GUI.SkillTreeGUI;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@CommandAlias("test")
public class TestCommand extends BaseCommand {

    @Default
    public void onRun(@NotNull Player player) {
        new SkillTreeGUI(SkillType.BLACKSMITH, player).open(player);
    }
}
