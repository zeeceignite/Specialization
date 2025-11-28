package com.minecraftcivilizations.specialization.Analytics;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.google.common.util.concurrent.AtomicDouble;
import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Data.MongoConnection;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.Skill;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.Specialization;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import com.minecraftcivilizations.specialization.Distance.Town;
import com.minecraftcivilizations.specialization.Distance.TownManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public record AnalyticsData(
    Timestamp timestamp,
    String serverName,
    
    // Server-wide metrics
    int serverPopulation,
    int serverDeathsInPeriod,
    int serverComplexItemsCraftedInPeriod,
    double gini,
    double invertShannon,
    Map<String, Integer> serverComplexItemsCraftedDetailsInPeriod,
    Map<SkillType, Integer> serverClassPopulation,
    Map<SkillType, Map<Integer, Integer>> serverPlayersPerSkillLevel,
    Map<String, Integer> serverUrbanAreaPopulation,
    Map<String, Integer> serverDeathCauses,
    
    // Town-specific metrics (for towns with 5+ beds)
    Map<String, TownSpecificData> townSpecificData
){

public record TownSpecificData(
        int townPopulation,
        int townDeathsInPeriod,
        int townComplexItemsCraftedInPeriod,
        Map<String, Integer> townComplexItemsCraftedDetailsInPeriod,
        Map<SkillType, Integer> townClassPopulation,
        Map<SkillType, Map<Integer, Integer>> townPlayersPerSkillLevel,
        Map<SkillType, Double> townClassMasteryPercentages,
        String townBiome,
        double distanceFromClosestTown,
        double distanceFromSpawn,
        long townAgeInHours
){};

public static ConcurrentHashMap<EntityDamageEvent.DamageCause, Integer> deaths = new ConcurrentHashMap<>();

public static void autoPoll(){
    Bukkit.getLogger().info("polling analytics");
    Bukkit.getAsyncScheduler().runAtFixedRate(Specialization.getInstance(), (_) -> {
        // Only record data if there are at least 10 online players
        if (Bukkit.getOnlinePlayers().size() >= 10) {
            AnalyticsData data = poll();
            if (data != null) {
                MongoConnection.getCollection(MongoConnection.Collections.ANALYTICS).insertOne(data);
                Bukkit.getLogger().info("polled analytics");
            }
        } else {
            Bukkit.getLogger().info("Skipping analytics poll - less than 10 players online");
        }
        wipe();
    }, 0, 5, TimeUnit.MINUTES);
}

private static void wipe(){
    deaths.clear();
    // Reset death counters for all online players
    Bukkit.getOnlinePlayers().stream()
            .map(player -> CoreUtil.getPlayer(player.getUniqueId()))
            .filter(Objects::nonNull)
            .forEach(customPlayer -> {
                customPlayer.getAnalyticPlayerData().resetDeathsForPeriod();
                customPlayer.getAnalyticPlayerData().resetComplexItemsForPeriod();
            });
}

private static AnalyticsData poll(){
    List<CustomPlayer> allPlayers = Bukkit.getOnlinePlayers().stream()
            .map(CoreUtil::getPlayer)
            .filter(Objects::nonNull)
            .toList();
    //change
    if (allPlayers.isEmpty()) {
        return null;
    }

    Timestamp now = new Timestamp(System.currentTimeMillis());
    
    // Server-wide metrics
    String serverName = SpecializationConfig.getServerConfig().get("SERVER_ANALYTIC", String.class);
    int serverPopulation = allPlayers.size();
    int serverDeathsInPeriod = allPlayers.stream()
            .mapToInt(player -> player.getAnalyticPlayerData().getDeathsThisPeriod())
            .sum();
    int serverComplexItemsCraftedInPeriod = allPlayers.stream()
            .mapToInt(player -> player.getAnalyticPlayerData().getComplexItemsCraftedThisPeriod())
            .sum();

    Map<String, Integer> serverComplexItemsCraftedDetailsInPeriod = new HashMap<>();
    for (CustomPlayer player : allPlayers) {
        player.getAnalyticPlayerData().getComplexItemsCraftedDetailsThisPeriod().forEach((material, count) -> 
            serverComplexItemsCraftedDetailsInPeriod.merge(material, count, Integer::sum));
    }

    Map<SkillType, Integer> serverClassPopulation = getSkillPopularity(allPlayers);
    Map<SkillType, Map<Integer, Integer>> serverPlayersPerSkillLevel = getPlayersPerSkillLevel(allPlayers);
    Map<String, Integer> serverUrbanAreaPopulation = getUrbanAreaPopulation();
    
    // Convert enum keys to strings to avoid Jackson serialization issues
    Map<String, Integer> serverDeathCausesAsStrings = new HashMap<>();
    for (Map.Entry<EntityDamageEvent.DamageCause, Integer> entry : deaths.entrySet()) {
        serverDeathCausesAsStrings.put(entry.getKey().toString(), entry.getValue());
    }

    // Town-specific data
    Map<String, TownSpecificData> townSpecificData = getTownSpecificData(allPlayers);


    List<Double> giniList = new ArrayList<>();
    List<Double> shannonList = new ArrayList<>();

    //for all players
    for(CustomPlayer player: allPlayers){
        int i = 1;
        double weightedSum = 0D;
        double H = 0D;

        List<Skill> toSort = new ArrayList<>(player.getSkills());
        toSort.sort(Comparator.comparingDouble(Skill::getXp)); // sorted list of skills

        for (Skill skill : toSort) {
            weightedSum += skill.getXp() * i; // this is the sum at the top of the gini fraction
            double Pi = skill.getXp() / player.getTotalXp(); // Pi value for shannon
            H += Pi * Math.log(Pi); // H value for shannon
            i++;
        }
        double gini = (2 * weightedSum / (7*player.getTotalXp())) - ((double) 8 /7);
        double normalizedH = H / Math.log(H);
        double inverted = 1 - normalizedH;

        giniList.add(gini);
        shannonList.add(inverted);
    }

    double gini = giniList.stream().mapToDouble(Double::doubleValue).average().getAsDouble();
    double shannon = shannonList.stream().mapToDouble(Double::doubleValue).average().getAsDouble();



    return new AnalyticsData(now,
            serverName,
            serverPopulation,
            serverDeathsInPeriod,
            serverComplexItemsCraftedInPeriod,
            gini,
            shannon,
            serverComplexItemsCraftedDetailsInPeriod,
            serverClassPopulation,
            serverPlayersPerSkillLevel,
            serverUrbanAreaPopulation,
            serverDeathCausesAsStrings,
            townSpecificData);
}

    private static Map<SkillType, Integer> getSkillPopularity(List<CustomPlayer> allPlayers){
        Map<SkillType, Integer> skillCounts = new HashMap<>();
        
        // Initialize all skill types with 0
        for (SkillType skillType : SkillType.values()) {
            skillCounts.put(skillType, 0);
        }
        
        // Count players for each skill type based on their highest skill
        for (CustomPlayer player : allPlayers) {
            SkillType highestSkill = null;
            int highestLevel = 0;
            
            // Find the player's highest skill level
            for (SkillType skillType : SkillType.values()) {
                int level = player.getSkillLevel(skillType);
                if (level > highestLevel) {
                    highestLevel = level;
                    highestSkill = skillType;
                }
            }
            
            // Count this player for their highest skill (if they have any skill levels)
            if (highestSkill != null && highestLevel > 0) {
                skillCounts.merge(highestSkill, 1, Integer::sum);
            }
        }
        
        return skillCounts;
    }
    
    private static Map<SkillType, Map<Integer, Integer>> getPlayersPerSkillLevel(List<CustomPlayer> allPlayers) {
        Map<SkillType, Map<Integer, Integer>> result = new HashMap<>();
        
        for (SkillType skillType : SkillType.values()) {
            Map<Integer, Integer> levelCounts = new HashMap<>();
            
            for (CustomPlayer player : allPlayers) {
                int level = player.getSkillLevel(skillType);
                levelCounts.merge(level, 1, Integer::sum);
            }
            
            result.put(skillType, levelCounts);
        }
        
        return result;
    }

    private static Map<String, Integer> getUrbanAreaPopulation(){
        HashMap<String, Integer> urbanAreas = new HashMap<>();
        int urbanRadius = 250; // 250 blocks radius
        
        for(Player player : Bukkit.getOnlinePlayers()){
            Location playerLoc = player.getLocation();
            
            // Count players within 250 blocks
            long nearbyPlayers = Bukkit.getOnlinePlayers().stream()
                    .filter(otherPlayer -> !otherPlayer.equals(player))
                    .filter(otherPlayer -> otherPlayer.getLocation().distance(playerLoc) <= urbanRadius)
                    .count();
            
            // Only consider areas with 10+ players as urban
            if (nearbyPlayers >= 10) {
                int x = (int)(playerLoc.getX() / 500); // 500 block grid for urban areas
                int z = (int)(playerLoc.getZ() / 500);
                String areaKey = x + "," + z;
                urbanAreas.merge(areaKey, 1, Integer::sum);
            }
        }
        return urbanAreas;
    }
    
    private static Map<String, TownSpecificData> getTownSpecificData(List<CustomPlayer> allPlayers) {
        Map<String, TownSpecificData> townData = new HashMap<>();
        
        // Get all towns with 3+ spawns (MIN_SPAWNS from TownManager)
        List<Town> eligibleTowns = TownManager.getTowns().stream()
                .filter(town -> town.getSpawnCount() >= 5)
                .toList();
        
        Bukkit.getLogger().info("Found " + TownManager.getTowns().size() + " total towns, " + eligibleTowns.size() + " eligible towns (3+ spawns)");
        
        for (Town town : eligibleTowns) {
            Bukkit.getLogger().info("Processing town with " + town.getSpawnCount() + " spawns at " + town.getCenterLocation());
            
            List<CustomPlayer> townPlayers = allPlayers.stream()
                    .filter(player -> {
                        Player bukkitPlayer = Bukkit.getPlayer(player.getUuid());
                        if (bukkitPlayer == null) {
                            Bukkit.getLogger().info("Player " + player.getUuid() + " not online, skipping");
                            return false;
                        }
                        
                        // Try multiple methods to get player location for town association
                        Location p      layerLocation = null;
                        
                        // Method 1: Check spawn location from TownManager
                        Location playerSpawn = TownManager.getPlayerSpawnLocations().get(player.getUuid());
                        if (playerSpawn != null) {
                            playerLocation = playerSpawn;
                            Bukkit.getLogger().info("Using TownManager spawn location for " + bukkitPlayer.getName());
                        } else {
                            // Method 2: Check bed spawn location
                            playerSpawn = bukkitPlayer.getBedSpawnLocation();
                            if (playerSpawn != null) {
                                playerLocation = playerSpawn;
                                Bukkit.getLogger().info("Using bed spawn location for " + bukkitPlayer.getName());
                            } else {
                                // Method 3: Use current location as fallback
                                playerLocation = bukkitPlayer.getLocation();
                                Bukkit.getLogger().info("Using current location for " + bukkitPlayer.getName() + " (no spawn/bed location found)");
                            }
                        }
                        
                        if (playerLocation == null) {
                            Bukkit.getLogger().warning("No location found for player " + bukkitPlayer.getName());
                            return false;
                        }
                        
                        double distance = playerLocation.distance(town.getCenterLocation());
                        boolean isInTown = distance <= 150; // TOWN_RADIUS from TownManager
                        
                        if (isInTown) {
                            Bukkit.getLogger().info("Player " + bukkitPlayer.getName() + " is in town (distance: " + distance + ")");
                        }
                        
                        return isInTown;
                    })
                    .toList();
            
            Bukkit.getLogger().info("Town has " + townPlayers.size() + " associated players");
            
            if (!townPlayers.isEmpty()) {
                TownSpecificData data = calculateTownSpecificData(town, townPlayers, eligibleTowns);
                String townKey = "Town_" + town.getSpawnCount() + "spawns_" + 
                    (int)town.getCenterLocation().getX() + "_" + (int)town.getCenterLocation().getZ();
                townData.put(townKey, data);
                Bukkit.getLogger().info("Added town data for: " + townKey);
            }
        }
        
        Bukkit.getLogger().info("Generated town data for " + townData.size() + " towns");
        return townData;
    }
    
    private static TownSpecificData calculateTownSpecificData(Town town, List<CustomPlayer> townPlayers, List<Town> allTowns) {
        int townPopulation = townPlayers.size();
        int townDeathsInPeriod = townPlayers.stream()
                .mapToInt(player -> player.getAnalyticPlayerData().getDeathsThisPeriod())
                .sum();
        int townComplexItemsCraftedInPeriod = townPlayers.stream()
                .mapToInt(player -> player.getAnalyticPlayerData().getComplexItemsCraftedThisPeriod())
                .sum();

        Map<String, Integer> townComplexItemsCraftedDetailsInPeriod = new HashMap<>();
        for (CustomPlayer player : townPlayers) {
            player.getAnalyticPlayerData().getComplexItemsCraftedDetailsThisPeriod().forEach((material, count) -> 
                townComplexItemsCraftedDetailsInPeriod.merge(material, count, Integer::sum));
        }
        
        Map<SkillType, Integer> townClassPopulation = getSkillPopularity(townPlayers);
        Map<SkillType, Map<Integer, Integer>> townPlayersPerSkillLevel = getPlayersPerSkillLevel(townPlayers);
        
        // Calculate average mastery percentages per class
        Map<SkillType, Double> townClassMasteryPercentages = getTownClassMasteryPercentages(townPlayers);
        
        // Get town center location
        Location townCenter = town.getCenterLocation();
        String townBiome = townCenter.getBlock().getBiome().toString();
        
        // Calculate distance to closest other town
        double distanceFromClosestTown = allTowns.stream()
                .filter(otherTown -> !otherTown.equals(town))
                .mapToDouble(otherTown -> townCenter.distance(otherTown.getCenterLocation()))
                .min()
                .orElse(0.0);

        // Distance from world spawn
        World world = townCenter.getWorld();
        Location worldSpawn = world.getSpawnLocation();
        double distanceFromSpawn = townCenter.distance(worldSpawn);
        
        // Calculate town age in hours
        long townAgeInHours = (System.currentTimeMillis() - town.getDiscoveredTime()) / (1000 * 60 * 60);
        
        return new TownSpecificData(
                townPopulation,
                townDeathsInPeriod,
                townComplexItemsCraftedInPeriod,
                townComplexItemsCraftedDetailsInPeriod,
                townClassPopulation,
                townPlayersPerSkillLevel,
                townClassMasteryPercentages,
                townBiome,
                distanceFromClosestTown,
                distanceFromSpawn,
                townAgeInHours
        );
    }
    
    private static Map<SkillType, Double> getTownClassMasteryPercentages(List<CustomPlayer> townPlayers) {
        Map<SkillType, Double> masteryPercentages = new HashMap<>();
        
        // Initialize all skill types with 0.0
        for (SkillType skillType : SkillType.values()) {
            masteryPercentages.put(skillType, 0.0);
        }
        
        // Calculate average mastery percentage for each skill type
        for (SkillType skillType : SkillType.values()) {
            List<Double> skillMasteryPercentages = townPlayers.stream()
                    .mapToDouble(player -> player.getPercentOfTotal(skillType))
                    .filter(Double::isFinite) // Filter out NaN and Infinity values
                    .boxed()
                    .toList();

            if (!skillMasteryPercentages.isEmpty()) {
                double averageMastery = skillMasteryPercentages.stream()
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0.0);

                // Additional safety check for NaN/Infinity before storing
                if (Double.isFinite(averageMastery)) {
                    masteryPercentages.put(skillType, Math.round(averageMastery * 100.0) / 100.0);
                } else {
                    masteryPercentages.put(skillType, 0.0); // Default to 0.0 for invalid values
                }
            }
        }
        
        return masteryPercentages;
    }

}
