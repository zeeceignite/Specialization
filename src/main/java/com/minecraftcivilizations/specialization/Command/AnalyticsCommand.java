package com.minecraftcivilizations.specialization.Command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import com.minecraftcivilizations.specialization.Analytics.AnalyticsData;
import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Data.MongoConnection;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import java.lang.reflect.Method;
import java.util.Map;

@CommandAlias("analytics|poll")
public class AnalyticsCommand extends BaseCommand {

    @Default
    @CommandPermission("specialization.analytics")
    public void onAnalytics(Player player) {
        String serverName = SpecializationConfig.getServerConfig().get("SERVER_ANALYTIC", String.class);
        
        player.sendMessage("§6=== Manual Analytics Poll ===");
        player.sendMessage("§eServer: §f" + serverName);
//change
        try {
            Method pollMethod = AnalyticsData.class.getDeclaredMethod("poll");
            pollMethod.setAccessible(true);
            AnalyticsData data = (AnalyticsData) pollMethod.invoke(null);

            if (data == null) {
                player.sendMessage("§cNo analytics data available (no online players)");
                return;
            }

            // Display server-wide metrics
            player.sendMessage("§e--- Server Metrics ---");
            player.sendMessage("§Gini: §f" + data.gini());
            player.sendMessage("§Inverted Shannon: §f" + data.invertShannon());


            player.sendMessage("§7Population: §f" + data.serverPopulation());
            player.sendMessage("§7Deaths this period: §f" + data.serverDeathsInPeriod());
            player.sendMessage("§7Complex Items Crafted: §f" + data.serverComplexItemsCraftedInPeriod());
            if (!data.serverComplexItemsCraftedDetailsInPeriod().isEmpty()) {
                player.sendMessage("§7Complex Items Details:");
                for (Map.Entry<String, Integer> entry : data.serverComplexItemsCraftedDetailsInPeriod().entrySet()) {
                    player.sendMessage("  §7" + entry.getKey() + ": §f" + entry.getValue());
                }
            }

            // Display class population
            player.sendMessage("§7Class Population:");
            for (Map.Entry<SkillType, Integer> entry : data.serverClassPopulation().entrySet()) {
                player.sendMessage("  §7" + entry.getKey() + ": §f" + entry.getValue());
            }

            // Display death causes
            if (!data.serverDeathCauses().isEmpty()) {
                player.sendMessage("§7Death Causes:");
                for (Map.Entry<String, Integer> entry : data.serverDeathCauses().entrySet()) {
                    player.sendMessage("  §7" + entry.getKey() + ": §f" + entry.getValue());
                }
            }

            // Display urban areas
            if (!data.serverUrbanAreaPopulation().isEmpty()) {
                player.sendMessage("§7Urban Areas:");
                for (Map.Entry<String, Integer> entry : data.serverUrbanAreaPopulation().entrySet()) {
                    player.sendMessage("  §7" + entry.getKey() + ": §f" + entry.getValue() + " players");
                }
            }

            // Display town data
            if (!data.townSpecificData().isEmpty()) {
                player.sendMessage("§e--- Town Data ---");
                for (Map.Entry<String, AnalyticsData.TownSpecificData> entry : data.townSpecificData().entrySet()) {
                    AnalyticsData.TownSpecificData townData = entry.getValue();
                    player.sendMessage("§7" + entry.getKey() + ":");
                    player.sendMessage("  §7Population: §f" + townData.townPopulation());
                    player.sendMessage("  §7Deaths: §f" + townData.townDeathsInPeriod());
                    player.sendMessage("  §7Complex Items Crafted: §f" + townData.townComplexItemsCraftedInPeriod());
                    if (!townData.townComplexItemsCraftedDetailsInPeriod().isEmpty()) {
                        player.sendMessage("    §7Complex Items Details:");
                        for (Map.Entry<String, Integer> itemEntry : townData.townComplexItemsCraftedDetailsInPeriod().entrySet()) {
                            player.sendMessage("      §7" + itemEntry.getKey() + ": §f" + itemEntry.getValue());
                        }
                    }
                    player.sendMessage("  §7Biome: §f" + townData.townBiome());
                    player.sendMessage("  §7Distance to closest town: §f" + String.format("%.1f", townData.distanceFromClosestTown()));
                    player.sendMessage("  §7Distance from spawn: §f" + String.format("%.1f", townData.distanceFromSpawn()));
                    player.sendMessage("  §7Age: §f" + townData.townAgeInHours() + " hours");
                }
            } else {
                player.sendMessage("§7No eligible towns found (need 5+ beds)");
            }

            // Save to database if requested
            player.sendMessage("§aSaving analytics data to database...");
            Bukkit.getAsyncScheduler().runNow(Bukkit.getPluginManager().getPlugin("Specialization"), (_) -> {
                MongoConnection.getCollection(MongoConnection.Collections.ANALYTICS).insertOne(data);
                Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("Specialization"), () -> {
                    player.sendMessage("§aAnalytics data saved successfully!");
                });
            });

        } catch (Exception e) {
            player.sendMessage("§cError polling analytics: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
