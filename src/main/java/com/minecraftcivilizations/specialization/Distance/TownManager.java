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
        Specialization.logger.info("[TownManager] Initializing TownManager...");
        Specialization.logger.info("[TownManager] Configuration: TOWN_RADIUS=" + TOWN_RADIUS + ", MIN_SPAWNS=" + MIN_SPAWNS + ", UPDATE_INTERVAL=" + UPDATE_INTERVAL + " ticks");
        startPeriodicUpdates();
        Specialization.logger.info("[TownManager] TownManager initialized - will update towns every 5 minutes");
        Specialization.logger.info("[TownManager] Debug logging enabled: " + debugLogging);
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
        long startTime = System.currentTimeMillis();
        
        if (debugLogging) {
            Specialization.logger.info("[TownManager] ===== STARTING TOWN UPDATE CYCLE =====");
            Specialization.logger.info("[TownManager] Current time: " + new java.util.Date());
            Specialization.logger.info("[TownManager] Previous town count: " + towns.size());
        }

        // Clear existing towns
        int previousTownCount = towns.size();
        towns.clear();
        
        // Collect all player spawn locations
        long collectStartTime = System.currentTimeMillis();
        collectPlayerSpawnLocations();
        long collectEndTime = System.currentTimeMillis();
        
        if (playerSpawnLocations.isEmpty()) {
            if (debugLogging) {
                Specialization.logger.info("[TownManager] No player spawn locations found - ending update cycle");
                Specialization.logger.info("[TownManager] ===== TOWN UPDATE CYCLE COMPLETE (NO DATA) =====");
            }
            return;
        }

        // Cluster spawn locations into towns
        long clusterStartTime = System.currentTimeMillis();
        List<List<Location>> clusters = clusterSpawnLocations();
        long clusterEndTime = System.currentTimeMillis();
        
        // Create towns from clusters
        long townCreationStartTime = System.currentTimeMillis();
        int townsCreated = 0;
        int clustersSkipped = 0;
        
        for (int i = 0; i < clusters.size(); i++) {
            List<Location> cluster = clusters.get(i);
            if (cluster.size() >= MIN_SPAWNS) {
                Location center = calculateCenter(cluster);
                Town town = new Town(center, cluster);
                towns.add(town);
                townsCreated++;
                
                if (debugLogging) {
                    Specialization.logger.info("[TownManager] Created town #" + townsCreated + " at " + 
                        formatLocation(center) + " with " + cluster.size() + " spawns");
                    
                    // Show some spawn locations for this town
                    if (cluster.size() <= 5) {
                        for (Location spawn : cluster) {
                            Specialization.logger.info("[TownManager]   - Spawn: " + formatLocation(spawn));
                        }
                    } else {
                        for (int j = 0; j < 3; j++) {
                            Specialization.logger.info("[TownManager]   - Spawn: " + formatLocation(cluster.get(j)));
                        }
                        Specialization.logger.info("[TownManager]   - ... and " + (cluster.size() - 3) + " more spawns");
                    }
                }
            } else {
                clustersSkipped++;
                if (debugLogging && clustersSkipped <= 3) {
                    Specialization.logger.info("[TownManager] Skipped cluster #" + (i + 1) + " - only " + 
                        cluster.size() + " spawns (minimum: " + MIN_SPAWNS + ")");
                }
            }
        }
        
        long townCreationEndTime = System.currentTimeMillis();
        long totalTime = System.currentTimeMillis() - startTime;

        if (debugLogging) {
            Specialization.logger.info("[TownManager] ===== TOWN UPDATE CYCLE COMPLETE =====");
            Specialization.logger.info("[TownManager] Performance metrics:");
            Specialization.logger.info("[TownManager]   - Total time: " + totalTime + "ms");
            Specialization.logger.info("[TownManager]   - Spawn collection: " + (collectEndTime - collectStartTime) + "ms");
            Specialization.logger.info("[TownManager]   - Clustering: " + (clusterEndTime - clusterStartTime) + "ms");
            Specialization.logger.info("[TownManager]   - Town creation: " + (townCreationEndTime - townCreationStartTime) + "ms");
            Specialization.logger.info("[TownManager] Results:");
            Specialization.logger.info("[TownManager]   - Previous towns: " + previousTownCount);
            Specialization.logger.info("[TownManager]   - New towns created: " + townsCreated);
            Specialization.logger.info("[TownManager]   - Clusters processed: " + clusters.size());
            Specialization.logger.info("[TownManager]   - Clusters skipped (too small): " + clustersSkipped);
            Specialization.logger.info("[TownManager]   - Total spawn locations: " + playerSpawnLocations.size());
        }
        
        // Always log the summary for server operators
        Specialization.logger.info("[TownManager] Town update complete: Found " + townsCreated + 
            " towns from " + playerSpawnLocations.size() + " player spawns (" + totalTime + "ms)");
    }

    /**
     * Collect spawn locations from all players (online and offline)
     */
    private void collectPlayerSpawnLocations() {
        if (debugLogging) {
            Specialization.logger.info("[TownManager] Starting spawn location collection...");
        }
        
        playerSpawnLocations.clear();
        
        // Get all players (online and offline)
        OfflinePlayer[] allPlayers = Bukkit.getOfflinePlayers();
        int playersProcessed = 0;
        int playersSkippedWorldSpawn = 0;
        int playersSkippedNoBed = 0;
        int playersSkippedWrongWorld = 0;
        
        if (debugLogging) {
            Specialization.logger.info("[TownManager] Processing " + allPlayers.length + " total players...");
        }
        
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
                    } else {
                        playersSkippedWorldSpawn++;
                        if (debugLogging && playersSkippedWorldSpawn <= 5) {
                            Specialization.logger.info("[TownManager] Ignoring " + player.getName() + "'s spawn - too close to world spawn (distance: " + 
                                String.format("%.1f", bedSpawn.distance(worldSpawn)) + ")");
                        }
                    }
                } else {
                    playersSkippedWrongWorld++;
                }
            } else {
                playersSkippedNoBed++;
            }
            
            if (spawnLocation != null && spawnLocation.getWorld() != null) {
                // Only consider overworld spawns
                if (spawnLocation.getWorld().getEnvironment() == World.Environment.NORMAL) {
                    playerSpawnLocations.put(player.getUniqueId(), spawnLocation);
                    playersProcessed++;
                    
                    if (debugLogging && playersProcessed <= 10) { // Only log first 10 to avoid spam
                        Specialization.logger.info("[TownManager] Added bed spawn for " + player.getName() + " at " + 
                            formatLocation(spawnLocation) + " (UUID: " + player.getUniqueId().toString().substring(0, 8) + "...)");
                    }
                } else {
                    playersSkippedWrongWorld++;
                    if (debugLogging && playersSkippedWrongWorld <= 3) {
                        Specialization.logger.info("[TownManager] Skipping " + player.getName() + " - spawn in " + 
                            spawnLocation.getWorld().getEnvironment() + " environment");
                    }
                }
            }
        }
        
        if (debugLogging) {
            Specialization.logger.info("[TownManager] Spawn collection complete:");
            Specialization.logger.info("[TownManager]   - Valid spawns collected: " + playersProcessed);
            Specialization.logger.info("[TownManager]   - Skipped (no bed): " + playersSkippedNoBed);
            Specialization.logger.info("[TownManager]   - Skipped (too close to world spawn): " + playersSkippedWorldSpawn);
            Specialization.logger.info("[TownManager]   - Skipped (wrong world/environment): " + playersSkippedWrongWorld);
            Specialization.logger.info("[TownManager]   - Total players processed: " + allPlayers.length);
        }
    }

    /**
     * Cluster spawn locations that are close together
     */
    private List<List<Location>> clusterSpawnLocations() {
        List<Location> locations = new ArrayList<>(playerSpawnLocations.values());
        List<List<Location>> clusters = new ArrayList<>();
        Set<Location> processed = new HashSet<>();
        
        if (debugLogging) {
            Specialization.logger.info("[TownManager] Starting clustering algorithm with " + locations.size() + " spawn locations");
            Specialization.logger.info("[TownManager] Clustering parameters: TOWN_RADIUS=" + TOWN_RADIUS + " blocks");
        }
        
        int clusterIndex = 0;
        
        for (Location location : locations) {
            if (processed.contains(location)) {
                continue;
            }
            
            clusterIndex++;
            if (debugLogging) {
                Specialization.logger.info("[TownManager] Processing cluster #" + clusterIndex + " starting from " + formatLocation(location));
            }
            
            // Start a new cluster
            List<Location> cluster = new ArrayList<>();
            Queue<Location> toProcess = new LinkedList<>();
            toProcess.add(location);
            
            int iterationsInCluster = 0;
            
            while (!toProcess.isEmpty()) {
                Location current = toProcess.poll();
                if (processed.contains(current)) {
                    continue;
                }
                
                iterationsInCluster++;
                processed.add(current);
                cluster.add(current);
                
                if (debugLogging && iterationsInCluster <= 5) {
                    Specialization.logger.info("[TownManager]   - Added location " + formatLocation(current) + " to cluster #" + clusterIndex);
                }
                
                // Find all nearby locations
                int nearbyFound = 0;
                for (Location other : locations) {
                    if (!processed.contains(other) && 
                        current.getWorld().equals(other.getWorld()) && 
                        current.distance(other) <= TOWN_RADIUS) {
                        toProcess.add(other);
                        nearbyFound++;
                        
                        if (debugLogging && nearbyFound <= 3) {
                            Specialization.logger.info("[TownManager]     - Found nearby spawn at " + formatLocation(other) + 
                                " (distance: " + String.format("%.1f", current.distance(other)) + ")");
                        }
                    }
                }
                
                if (debugLogging && nearbyFound > 3) {
                    Specialization.logger.info("[TownManager]     - Found " + (nearbyFound - 3) + " more nearby spawns...");
                }
            }
            
            if (!cluster.isEmpty()) {
                clusters.add(cluster);
                if (debugLogging) {
                    Specialization.logger.info("[TownManager] Completed cluster #" + clusterIndex + " with " + cluster.size() + 
                        " spawns near " + formatLocation(cluster.get(0)) + 
                        (cluster.size() >= MIN_SPAWNS ? " [ELIGIBLE FOR TOWN]" : " [TOO SMALL]"));
                }
            }
        }
        
        if (debugLogging) {
            Specialization.logger.info("[TownManager] Clustering complete: Found " + clusters.size() + " total clusters");
            int eligibleClusters = (int) clusters.stream().filter(c -> c.size() >= MIN_SPAWNS).count();
            Specialization.logger.info("[TownManager] Eligible clusters (>=" + MIN_SPAWNS + " spawns): " + eligibleClusters);
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