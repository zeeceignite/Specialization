package com.minecraftcivilizations.specialization.Distance;

import com.minecraftcivilizations.specialization.Specialization;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class TownManager {
    @Getter
    public static TownManager instance;
    
    private static final List<Town> towns = Collections.synchronizedList(new ArrayList<>());
    private static final Map<UUID, Location> playerSpawnLocations = new ConcurrentHashMap<>();
    
    // Configuration
    private static final int TOWN_RADIUS = 150; // Distance to consider spawns as same town
    private static final int MIN_SPAWNS = 5;    // Minimum spawns needed to form a town
    private static final int UPDATE_INTERVAL = 6000; // 5 minutes in ticks (20 ticks = 1 second)
    
    private BukkitTask updateTask;
    private boolean debugLogging = true;

    public TownManager() {
        instance = this;
        startPeriodicUpdates();
        Specialization.logger.info("TownManager initialized - will update towns every 5 minutes");
    }

    /**
     * Start the periodic town detection system
     */
    public void startPeriodicUpdates() {
        if (updateTask != null) {
            updateTask.cancel();
        }

        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateTowns();
            }
        }.runTaskTimer(Specialization.getInstance(), 100L, UPDATE_INTERVAL); // Start after 5 seconds, repeat every 5 minutes
        
        if (debugLogging) {
            Specialization.logger.info("Started periodic town updates every 5 minutes");
        }
    }

    /**
     * Stop the periodic updates
     */
    public void stopPeriodicUpdates() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        if (debugLogging) {
            Specialization.logger.info("Stopped periodic town updates");
        }
    }

    /**
     * Main method that collects all player spawn locations and clusters them into towns
     */
    public void updateTowns() {
        if (debugLogging) {
            Specialization.logger.info("[TownManager] Starting town update cycle...");
        }

        // Clear existing towns
        towns.clear();
        
        // Collect all player spawn locations
        collectPlayerSpawnLocations();
        
        if (playerSpawnLocations.isEmpty()) {
            if (debugLogging) {
                Specialization.logger.info("[TownManager] No player spawn locations found");
            }
            return;
        }

        // Cluster spawn locations into towns
        List<List<Location>> clusters = clusterSpawnLocations();
        
        // Create towns from clusters
        int townsCreated = 0;
        for (List<Location> cluster : clusters) {
            if (cluster.size() >= MIN_SPAWNS) {
                Location center = calculateCenter(cluster);
                Town town = new Town(center, cluster);
                towns.add(town);
                townsCreated++;
                
                if (debugLogging) {
                    Specialization.logger.info(String.format("[TownManager] Created town at %s with %d spawns", 
                        formatLocation(center), cluster.size()));
                }
            }
        }

        if (debugLogging) {
            Specialization.logger.info(String.format("[TownManager] Update complete: %d towns created from %d spawn locations", 
                townsCreated, playerSpawnLocations.size()));
        }
        
        Specialization.logger.info(String.format("Town update complete: Found %d towns from %d player spawns", 
            townsCreated, playerSpawnLocations.size()));
    }

    /**
     * Collect spawn locations from all players (online and offline)
     */
    private void collectPlayerSpawnLocations() {
        playerSpawnLocations.clear();
        
        // Get all players (online and offline)
        OfflinePlayer[] allPlayers = Bukkit.getOfflinePlayers();
        int playersProcessed = 0;
        
        for (OfflinePlayer player : allPlayers) {
            Location spawnLocation = null;
            
            // Only get bed spawn location - ignore default world spawn
            if (player.getBedSpawnLocation() != null) {
                Location bedSpawn = player.getBedSpawnLocation();
                World world = bedSpawn.getWorld();
                
                if (world != null) {
                    // Check if this is NOT the default world spawn location
                    Location worldSpawn = world.getSpawnLocation();
                    
                    // Only use bed spawn if it's different from world spawn (with some tolerance)
                    if (bedSpawn.distance(worldSpawn) > 10) { // 10 block tolerance
                        spawnLocation = bedSpawn;
                    } else if (debugLogging && playersProcessed < 5) {
                        Specialization.logger.info(String.format("[TownManager] Ignoring %s's spawn - too close to world spawn", 
                            player.getName()));
                    }
                }
            }
            
            if (spawnLocation != null && spawnLocation.getWorld() != null) {
                // Only consider overworld spawns
                if (spawnLocation.getWorld().getEnvironment() == World.Environment.NORMAL) {
                    playerSpawnLocations.put(player.getUniqueId(), spawnLocation);
                    playersProcessed++;
                    
                    if (debugLogging && playersProcessed <= 10) { // Only log first 10 to avoid spam
                        Specialization.logger.info(String.format("[TownManager] Added bed spawn for %s at %s", 
                            player.getName(), formatLocation(spawnLocation)));
                    }
                }
            }
        }
        
        if (debugLogging) {
            Specialization.logger.info(String.format("[TownManager] Collected %d valid bed spawn locations from %d total players", 
                playersProcessed, allPlayers.length));
        }
    }

    /**
     * Cluster spawn locations that are close together
     */
    private List<List<Location>> clusterSpawnLocations() {
        List<Location> locations = new ArrayList<>(playerSpawnLocations.values());
        List<List<Location>> clusters = new ArrayList<>();
        Set<Location> processed = new HashSet<>();
        
        for (Location location : locations) {
            if (processed.contains(location)) {
                continue;
            }
            
            // Start a new cluster
            List<Location> cluster = new ArrayList<>();
            Queue<Location> toProcess = new LinkedList<>();
            toProcess.add(location);
            
            while (!toProcess.isEmpty()) {
                Location current = toProcess.poll();
                if (processed.contains(current)) {
                    continue;
                }
                
                processed.add(current);
                cluster.add(current);
                
                // Find all nearby locations
                for (Location other : locations) {
                    if (!processed.contains(other) && 
                        current.getWorld().equals(other.getWorld()) && 
                        current.distance(other) <= TOWN_RADIUS) {
                        toProcess.add(other);
                    }
                }
            }
            
            if (!cluster.isEmpty()) {
                clusters.add(cluster);
                if (debugLogging) {
                    Specialization.logger.info(String.format("[TownManager] Found cluster with %d spawns near %s", 
                        cluster.size(), formatLocation(cluster.get(0))));
                }
            }
        }
        
        return clusters;
    }

    /**
     * Calculate the center point of a group of locations
     */
    private Location calculateCenter(List<Location> locations) {
        if (locations.isEmpty()) return null;

        double totalX = 0, totalY = 0, totalZ = 0;
        World world = locations.get(0).getWorld();

        for (Location loc : locations) {
            totalX += loc.getX();
            totalY += loc.getY();
            totalZ += loc.getZ();
        }

        return new Location(world,
                totalX / locations.size(),
                totalY / locations.size(),
                totalZ / locations.size());
    }

    /**
     * Manually trigger a town update (for commands)
     */
    public void scanAllPlayersForTowns() {
        Specialization.logger.info("Manual town scan triggered...");
        updateTowns();
    }

    /**
     * Get detection statistics
     */
    public void getDetectionStats() {
        Specialization.logger.info("=== Town Detection Statistics ===");
        Specialization.logger.info("Total towns: " + towns.size());
        Specialization.logger.info("Player spawns tracked: " + playerSpawnLocations.size());
        Specialization.logger.info("Update interval: 5 minutes");
        Specialization.logger.info("Debug logging: " + (debugLogging ? "Enabled" : "Disabled"));
        Specialization.logger.info("Town detection radius: " + TOWN_RADIUS + " blocks");
        Specialization.logger.info("Minimum spawns for town: " + MIN_SPAWNS);

        if (!towns.isEmpty()) {
            Specialization.logger.info("Current towns:");
            for (int i = 0; i < Math.min(towns.size(), 10); i++) { // Show max 10 towns
                Town town = towns.get(i);
                Specialization.logger.info(String.format("  %d. %s (%d spawns)", 
                    i + 1, formatLocation(town.getCenterLocation()), town.getBedCount()));
            }
            if (towns.size() > 10) {
                Specialization.logger.info("  ... and " + (towns.size() - 10) + " more towns");
            }
        }
    }

    /**
     * Enable or disable debug logging
     */
    public void setDebugLogging(boolean enabled) {
        this.debugLogging = enabled;
        if (enabled) {
            Specialization.logger.info("Town detection debug logging enabled");
        } else {
            Specialization.logger.info("Town detection debug logging disabled");
        }
    }

    /**
     * Cleanup when plugin is disabled
     */
    public void cleanup() {
        stopPeriodicUpdates();
        playerSpawnLocations.clear();
        towns.clear();
    }

    /**
     * Get all discovered towns
     */
    public static List<Town> getTowns() {
        return new ArrayList<>(towns);
    }

    /**
     * Get all player spawn locations
     */
    public static Map<UUID, Location> getPlayerSpawnLocations() {
        return new HashMap<>(playerSpawnLocations);
    }

    /**
     * Format a location for logging
     */
    public static String formatLocation(Location loc) {
        return String.format("%.1f, %.1f, %.1f in %s",
                loc.getX(), loc.getY(), loc.getZ(), loc.getWorld().getName());
    }
}