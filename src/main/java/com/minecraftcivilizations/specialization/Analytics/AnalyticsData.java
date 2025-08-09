package com.minecraftcivilizations.specialization.Analytics;

import com.minecraftcivilizations.specialization.Data.MongoConnection;
import com.minecraftcivilizations.specialization.Distance.Town;
import com.minecraftcivilizations.specialization.Distance.TownManager;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.Specialization;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public record AnalyticsData(
        Timestamp timestamp,
        int onlinePlayers,
        int totalDeaths,
        Map<String,Integer> playerDensity,
        Map<SkillType, Integer> classPopularity,
        List<Town> towns,
        Map<EntityDamageEvent.DamageCause, Integer> deathCauses
){

    public static ConcurrentHashMap<EntityDamageEvent.DamageCause, Integer> deaths = new ConcurrentHashMap<>();

    public static void autoPoll(){
        Bukkit.getLogger().info("polling analytics");
        Bukkit.getAsyncScheduler().runAtFixedRate(Specialization.getInstance(), (_) -> {
            MongoConnection.getCollection(MongoConnection.Collections.ANALYTICS).insertOne(poll());
            wipe();
            Bukkit.getLogger().info("polled analytics");
        }, 0, 5, TimeUnit.MINUTES);
    }

    private static void wipe(){
        deaths.clear();
    }

    private static AnalyticsData poll(){
        List<CustomPlayer> allPlayers = Bukkit.getOnlinePlayers().stream().map(CoreUtil::getPlayer).filter(Objects::nonNull) .toList();
        List<CustomPlayer.AnalyticPlayerData> allData = allPlayers.stream().map(CustomPlayer::getAnalyticPlayerData).filter(Objects::nonNull).toList();

        Timestamp now = new Timestamp(System.currentTimeMillis());
        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        int totalDeaths = allData.stream().mapToInt(CustomPlayer.AnalyticPlayerData::getDeaths).sum();
        Map<String, Integer> playerDensity = getPlayerDensity();
        Map<SkillType, Integer> totalSkills = allPlayers.stream()
                .flatMap(player -> Arrays.stream(SkillType.values()).map(skillType -> new AbstractMap.SimpleEntry<>(skillType, player.getSkillLevel(skillType))))
                .collect(
                        Collectors.toMap(
                                AbstractMap.SimpleEntry::getKey,
                                Map.Entry::getValue,
                                Integer::sum)
                );

        return new AnalyticsData(now, onlinePlayers, totalDeaths, playerDensity, totalSkills, TownManager.getTowns(), deaths);
    }

    private static Map<String, Integer> getPlayerDensity(){
        HashMap<String, Integer> playerDensity = new HashMap<>();
        int chunkAmount = 50;
        for(Player player : Bukkit.getOnlinePlayers()){
            int x = (int)(player.getLocation().getX() / chunkAmount);
            int z = (int)(player.getLocation().getZ() / chunkAmount);

            playerDensity.compute(x + "," + z,
                    (_, value) -> value == null ? 0 : value + 1);
        }
        return playerDensity;
    }

}
