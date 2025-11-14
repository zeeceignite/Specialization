package com.minecraftcivilizations.specialization.Command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.minecraftcivilizations.specialization.Listener.Player.XpGainMonitor;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import org.bukkit.entity.Player;

@CommandAlias("xpmonitor|xpm")
@Description("Manage XP gain thresholds and alert cooldowns")
public class XPMonitoringCommand extends BaseCommand {

    // --- GET ---
    @Subcommand("get|g")
    @Syntax("<skill>")
    @Description("Get current threshold and cooldown for a skill")
    @CommandPermission("civlabs.xpmonitor")
    @CommandCompletion("@classes @monitorTypes")
    public void onGet(Player sender, SkillType skill, @Optional String type) {
        if (type == null) {
            double threshold = XpGainMonitor.getThreshold(skill);
            long cooldown = XpGainMonitor.getCooldown(skill);
            sender.sendMessage(skill.name() + " Threshold: " + threshold + ", Cooldown: " + cooldown + "s");
            return;
        }

        switch (type.toLowerCase()) {
            case "threshold" -> sender.sendMessage(skill.name() + " Threshold: " + XpGainMonitor.getThreshold(skill));
            case "cooldown"  -> sender.sendMessage(skill.name() + " Cooldown: " + XpGainMonitor.getCooldown(skill) + "s");
            default -> sender.sendMessage("Invalid type: must be 'threshold' or 'cooldown'");
        }
    }


    // --- SET ---
    @Subcommand("set|s")
    @Syntax("<skill> <threshold|cooldown> <value>")
    @Description("Set threshold or cooldown for a skill")
    @CommandPermission("civlabs.xpmonitor")
    @CommandCompletion("@classes threshold|cooldown")
    public void onSet(Player sender, SkillType skill, String type, double value) {
        switch (type.toLowerCase()) {
            case "threshold" -> {
                XpGainMonitor.setThreshold(skill, value);
                sender.sendMessage("Set " + skill.name() + " threshold to " + value);
            }
            case "cooldown" -> {
                XpGainMonitor.setCooldown(skill, (long) value);
                sender.sendMessage("Set " + skill.name() + " cooldown to " + (long) value + "s");
            }
            default -> sender.sendMessage("Invalid type: must be 'threshold' or 'cooldown'");
        }
    }

    // --- RESET ---
    @Subcommand("reset|r")
    @Syntax("<skill>")
    @Description("Reset threshold and cooldown to defaults from config")
    @CommandPermission("civlabs.xpmonitor")
    @CommandCompletion("@classes")
    public void onReset(Player sender, SkillType skill) {
        XpGainMonitor.init(); // reloads defaults from config
        sender.sendMessage(skill.name() + " threshold and cooldown reset to defaults.");
    }

    // --- SAVE ---
    @Subcommand("save")
    @Description("Save current thresholds and cooldowns to disk")
    @CommandPermission("civlabs.xpmonitor")
    public void onSave(Player sender) {
        XpGainMonitor.saveConfigToDisk();
        sender.sendMessage("XP monitor config saved to disk.");
    }

    // --- DEFAULT / HELP ---
    @Default
    @Description("Shows XPMonitor command usage")
    @CommandPermission("civlabs.xpmonitor")
    public void onDefault(Player sender) {
        sender.sendMessage("§7==== §eXP Monitor Commands §7====");
        sender.sendMessage("§e/xpm get <skill> §7- Show current threshold and cooldown");
        sender.sendMessage("§e/xpm set <skill> <threshold|cooldown> <value> §7- Update threshold or cooldown");
        sender.sendMessage("§e/xpm reset <skill> §7- Reset values to defaults from config");
        sender.sendMessage("§e/xpm save §7- Save current values to disk");
    }
}
