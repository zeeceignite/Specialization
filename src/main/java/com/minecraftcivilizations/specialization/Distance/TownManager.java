package com.minecraftcivilizations.specialization.Distance;

import com.minecraftcivilizations.specialization.Specialization;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class TownManager implements Listener {
    @Getter
    public static TownManager instance;
    private static final List<Town> towns = Collections.synchronizedList(new ArrayList<>());
    private static final Map<UUID, Location> playerSpawnLocations = new ConcurrentHashMap<>();
    private static final Map<UUID, Location> lastScanLocations = new ConcurrentHashMap<>();
    private static final int TOWN_RADIUS = 150;
    private static final int MIN_BEDS = 5;
    private static final int MOVEMENT_SCAN_THRESHOLD = 100;
    private static final int SCAN_RADIUS_REDUCTION = 2;

    private BukkitTask periodicScanTask;
    private boolean debugLogging = true;

    public TownManager() {
        instance = this;
        startPeriodicScanning();
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (debugLogging) {
            Specialization.logger.info("[DEBUG] PlayerRespawnEvent triggered for " + event.getPlayer().getName());
        }
        
        Location spawnLocation = event.getRespawnLocation();
        UUID playerId = event.getPlayer().getUniqueId();

        playerSpawnLocations.put(playerId, spawnLocation);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (debugLogging) {
                    Specialization.logger.info("[DEBUG] Processing respawn scan for " + event.getPlayer().getName() + 
                        " at " + formatLocation(spawnLocation));
                }
                scanForTownsAroundLocation(spawnLocation, "Player Respawn: " + event.getPlayer().getName());
                updateTownsList();
                getDetectionStats();
            }
        }.runTaskAsynchronously(Specialization.getInstance());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (debugLogging) {
            Specialization.logger.info("[DEBUG] PlayerJoinEvent triggered for " + event.getPlayer().getName());
        }
        
        Location loc = event.getPlayer().getLocation();
        UUID playerId = event.getPlayer().getUniqueId();

        new BukkitRunnable() {
            @Override
            public void run() {
                if (debugLogging) {
                    Specialization.logger.info("[DEBUG] Processing join scan for " + event.getPlayer().getName() + 
                        " at " + formatLocation(loc));
                }
                scanForTownsAroundLocation(loc, "Player Join: " + event.getPlayer().getName());
                updateTownsList();
                getDetectionStats();
            }
        }.runTaskAsynchronously(Specialization.getInstance());
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        Location from = event.getFrom();
        UUID playerId = event.getPlayer().getUniqueId();

        if (to == null) return;

        Location lastScan = lastScanLocations.get(playerId);

        boolean shouldScan = false;
        if (lastScan == null) {
            shouldScan = true;
        } else if (!lastScan.getWorld().equals(to.getWorld())) {
            shouldScan = true;
        } else if (lastScan.distance(to) > MOVEMENT_SCAN_THRESHOLD) {
            shouldScan = true;
        }

        if (shouldScan) {
            if (debugLogging) {
                Specialization.logger.info("[DEBUG] PlayerMoveEvent triggered scan for " + event.getPlayer().getName() + 
                    " - moved " + (lastScan != null ? String.format("%.1f", lastScan.distance(to)) : "first scan") + " blocks");
            }
            
            lastScanLocations.put(playerId, to.clone());

            new BukkitRunnable() {
                @Override
                public void run() {
                    scanForTownsAroundLocation(to, "Player Movement: " + event.getPlayer().getName());
                    updateTownsList();
                    getDetectionStats();
                }
            }.runTaskAsynchronously(Specialization.getInstance());
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (debugLogging) {
            Specialization.logger.info("[DEBUG] BlockPlaceEvent triggered for " + event.getPlayer().getName() + 
                " placing " + event.getBlock().getType() + " at " + formatLocation(event.getBlock().getLocation()));
        }
        
        Location blockLocation = event.getBlock().getLocation();
        Player player = event.getPlayer();

        new BukkitRunnable() {
            @Override
            public void run() {
                scanForTownsAroundLocation(blockLocation, "Block Place: " + player.getName());
                updateTownsList();
                if (debugLogging) {
                    getDetectionStats();
                }
            }
        }.runTaskAsynchronously(Specialization.getInstance());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (debugLogging) {
            Specialization.logger.info("[DEBUG] BlockBreakEvent triggered for " + event.getPlayer().getName() + 
                " breaking " + event.getBlock().getType() + " at " + formatLocation(event.getBlock().getLocation()));
        }
        
        Location blockLocation = event.getBlock().getLocation();
        Player player = event.getPlayer();

        new BukkitRunnable() {
            @Override
            public void run() {
                scanForTownsAroundLocation(blockLocation, "Block Break: " + player.getName());
                updateTownsList();
                if (debugLogging) {
                    getDetectionStats();
                }
            }
        }.runTaskAsynchronously(Specialization.getInstance());
    }

    @EventHandler
    public void onPlayerSleep(PlayerBedEnterEvent event) {
        if (event.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.OK) {
            return; // Only process successful bed entries
        }
        
        if (debugLogging) {
            Specialization.logger.info("[DEBUG] PlayerBedEnterEvent triggered for " + event.getPlayer().getName() + 
                " sleeping at " + formatLocation(event.getBed().getLocation()));
        }
        
        Location bedLocation = event.getBed().getLocation();
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Store this as a potential spawn location
        playerSpawnLocations.put(playerId, bedLocation);

        new BukkitRunnable() {
            @Override
            public void run() {
                scanForTownsAroundLocation(bedLocation, "Player Sleep: " + player.getName());
                updateTownsList();
                if (debugLogging) {
                    getDetectionStats();
                }
            }
        }.runTaskAsynchronously(Specialization.getInstance());
    }

    public void startPeriodicScanning() {
        if (periodicScanTask != null) {
            periodicScanTask.cancel();
        }

        periodicScanTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    scanForTownsAroundLocation(player.getLocation(), "Periodic Scan: " + player.getName());
                }
                updateTownsList();

                if (debugLogging) {
                    Specialization.logger.info("Periodic scan completed. Total towns: " + towns.size());
                    getDetectionStats();
                }
            }
        }.runTaskTimerAsynchronously(Specialization.getInstance(), 6000, 6000);
    }

    public void stopPeriodicScanning() {
        if (periodicScanTask != null) {
            periodicScanTask.cancel();
            periodicScanTask = null;
        }
    }

    public static void scanAllPlayersForTowns() {
        if (instance != null && instance.debugLogging) {
            Specialization.logger.info("[DEBUG] Starting comprehensive town scan...");
        }

        towns.clear();

        OfflinePlayer[] allPlayers = Bukkit.getOfflinePlayers();
        Set<Location> scannedLocations = new HashSet<>();
        int playersProcessed = 0;
        int locationsScanned = 0;

        if (instance != null && instance.debugLogging) {
            Specialization.logger.info("[DEBUG] Processing " + allPlayers.length + " total players for town detection");
        }

        for (OfflinePlayer player : allPlayers) {
            playersProcessed++;
            
            // Check bed spawn location
            if (player.getBedSpawnLocation() != null) {
                Location bedLocation = player.getBedSpawnLocation();
                playerSpawnLocations.put(player.getUniqueId(), bedLocation);

                if (!isLocationNearScanned(bedLocation, scannedLocations, TOWN_RADIUS / SCAN_RADIUS_REDUCTION)) {
                    if (instance != null && instance.debugLogging) {
                        Specialization.logger.info("[DEBUG] Scanning bed location for " + player.getName() + 
                            " at " + formatLocation(bedLocation));
                    }
                    scanForTownsAroundLocation(bedLocation, "Comprehensive Scan: " + player.getName());
                    scannedLocations.add(bedLocation);
                    locationsScanned++;
                }
            }
            
            // Also check if player is online and scan their current location
            if (player.isOnline() && player.getPlayer() != null) {
                Location currentLocation = player.getPlayer().getLocation();
                if (!isLocationNearScanned(currentLocation, scannedLocations, TOWN_RADIUS / SCAN_RADIUS_REDUCTION)) {
                    if (instance != null && instance.debugLogging) {
                        Specialization.logger.info("[DEBUG] Scanning current location for online player " + player.getName() + 
                            " at " + formatLocation(currentLocation));
                    }
                    scanForTownsAroundLocation(currentLocation, "Online Player Scan: " + player.getName());
                    scannedLocations.add(currentLocation);
                    locationsScanned++;
                }
            }
        }

        updateTownsList();
        
        if (instance != null && instance.debugLogging) {
            Specialization.logger.info("[DEBUG] Comprehensive scan completed:");
            Specialization.logger.info("[DEBUG] - Processed " + playersProcessed + " players");
            Specialization.logger.info("[DEBUG] - Scanned " + locationsScanned + " unique locations");
            Specialization.logger.info("[DEBUG] - Found " + towns.size() + " towns");
        }
        
        Specialization.logger.info("Comprehensive scan completed. Found " + towns.size() + " towns.");
    }

    public static void scanEntireWorld() {
        Specialization.logger.info("Starting systematic world scan...");

        new BukkitRunnable() {
            @Override
            public void run() {
                for (World world : Bukkit.getWorlds()) {
                    if (world.getEnvironment() != World.Environment.NORMAL) {
                        continue;
                    }

                    Specialization.logger.info("Scanning world: " + world.getName());

                    int step = TOWN_RADIUS;
                    int worldSize = 5000;

                    for (int x = -worldSize; x <= worldSize; x += step) {
                        for (int z = -worldSize; z <= worldSize; z += step) {
                            int y = world.getHighestBlockYAt(x, z);
                            if (y < world.getMinHeight()) y = 64;

                            Location scanPoint = new Location(world, x, y, z);
                            scanForTownsAroundLocation(scanPoint, "Systematic World Scan");
                        }
                    }
                }

                updateTownsList();
                Specialization.logger.info("Systematic world scan completed. Total towns: " + towns.size());
            }
        }.runTaskAsynchronously(Specialization.getInstance());
    }

    private static boolean isLocationNearScanned(Location location, Set<Location> scannedLocations, int radius) {
        for (Location scanned : scannedLocations) {
            if (scanned.getWorld().equals(location.getWorld()) &&
                    scanned.distance(location) <= radius) {
                return true;
            }
        }
        return false;
    }

    private static void scanForTownsAroundLocation(Location centerLocation, String source) {
        World world = centerLocation.getWorld();
        if (world == null) return;

        if (instance != null && instance.debugLogging) {
            Specialization.logger.info(String.format("[DEBUG] [%s] Starting scan at %s", 
                source, formatLocation(centerLocation)));
        }

        List<Location> bedsInArea = findBedsInRadius(centerLocation, TOWN_RADIUS);

        if (instance != null && instance.debugLogging && bedsInArea.size() > 0) {
            Specialization.logger.info(String.format("[DEBUG] [%s] Scanning %s: found %d beds",
                    source, formatLocation(centerLocation), bedsInArea.size()));
        }

        if (bedsInArea.size() >= MIN_BEDS) {
            boolean townExists = false;
            synchronized (towns) {
                for (Town existingTown : towns) {
                    if (existingTown.getCenterLocation().getWorld().equals(world) &&
                            existingTown.getCenterLocation().distance(centerLocation) <= TOWN_RADIUS) {
                        if (bedsInArea.size() > existingTown.getBedCount()) {
                            existingTown.updateBeds(bedsInArea);
                            if (instance != null && instance.debugLogging) {
                                Specialization.logger.info(String.format("[DEBUG] [%s] Updated existing town at %s with %d beds",
                                        source, formatLocation(existingTown.getCenterLocation()), bedsInArea.size()));
                            }
                        }
                        townExists = true;
                        break;
                    }
                }
            }

            if (!townExists) {
                Location townCenter = calculateTownCenter(bedsInArea);
                Town newTown = new Town(townCenter, bedsInArea);
                towns.add(newTown);

                if (instance != null && instance.debugLogging) {
                    Specialization.logger.info(String.format("[DEBUG] [%s] Created new town at %s with %d beds",
                            source, formatLocation(townCenter), bedsInArea.size()));
                }
            }
        } else if (instance != null && instance.debugLogging && bedsInArea.size() > 0) {
            Specialization.logger.info(String.format("[DEBUG] [%s] Found %d beds at %s but need %d minimum for town",
                    source, bedsInArea.size(), formatLocation(centerLocation), MIN_BEDS));
        }
    }

    private static void scanForTownsAroundLocation(Location centerLocation) {
        scanForTownsAroundLocation(centerLocation, "Legacy Scan");
    }

    private static List<Location> findBedsInRadius(Location center, int radius) {
        List<Location> beds = new ArrayList<>();
        World world = center.getWorld();
        if (world == null) return beds;

        if (instance != null && instance.debugLogging) {
            Specialization.logger.info(String.format("[DEBUG] Starting bed search at %s with radius %d", 
                formatLocation(center), radius));
        }

        int chunkRadius = (radius / 16) + 1;
        int centerChunkX = center.getBlockX() >> 4;
        int centerChunkZ = center.getBlockZ() >> 4;
        
        int chunksScanned = 0;
        int blocksScanned = 0;

        for (int chunkX = centerChunkX - chunkRadius; chunkX <= centerChunkX + chunkRadius; chunkX++) {
            for (int chunkZ = centerChunkZ - chunkRadius; chunkZ <= centerChunkZ + chunkRadius; chunkZ++) {

                boolean wasLoaded = world.isChunkLoaded(chunkX, chunkZ);
                if (!wasLoaded) {
                    world.loadChunk(chunkX, chunkZ, true);
                }
                
                chunksScanned++;

                // Scan each block position in the chunk
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        int worldX = (chunkX << 4) + x;
                        int worldZ = (chunkZ << 4) + z;
                        
                        // Check if this position is within our radius
                        double distanceSquared = Math.pow(worldX - center.getX(), 2) + Math.pow(worldZ - center.getZ(), 2);
                        if (distanceSquared > radius * radius) {
                            continue;
                        }

                        int highestY = world.getHighestBlockYAt(worldX, worldZ);
                        int minY = Math.max(world.getMinHeight(), highestY - 50);
                        int maxY = Math.min(world.getMaxHeight() - 1, highestY + 10);
                        
                        for (int y = maxY; y >= minY; y--) {
                            Block block = world.getBlockAt(worldX, y, worldZ);
                            blocksScanned++;
                            
                            if (isBed(block)) {
                                Location bedLoc = block.getLocation();
                                // Double-check distance calculation with actual location
                                if (center.distance(bedLoc) <= radius) {
                                    beds.add(bedLoc);
                                    if (instance != null && instance.debugLogging) {
                                        Specialization.logger.info(String.format("[DEBUG] Found bed at %s (distance: %.1f)", 
                                            formatLocation(bedLoc), center.distance(bedLoc)));
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Unload the chunk if we loaded it (to prevent memory issues)
                if (!wasLoaded) {
                    world.unloadChunk(chunkX, chunkZ, true);
                }
            }
        }

        if (instance != null && instance.debugLogging) {
            Specialization.logger.info(String.format("[DEBUG] Bed search completed: scanned %d chunks, %d blocks, found %d beds", 
                chunksScanned, blocksScanned, beds.size()));
        }

        return beds;
    }

    private static boolean isBed(Block block) {
        Material material = block.getType();
        
        // Check if it's a bed material by name (covers all bed colors)
        if (!material.name().endsWith("_BED")) {
            return false;
        }
        
        // Additional validation to ensure it's actually a bed block
        try {
            // Try to get bed data - this will fail if it's not actually a bed
            if (block.getBlockData() instanceof org.bukkit.block.data.type.Bed bed) {
                // Only count bed heads to avoid counting the same bed twice
                boolean isHead = bed.getPart() == org.bukkit.block.data.type.Bed.Part.HEAD;
                if (instance != null && instance.debugLogging && isHead) {
                    Specialization.logger.info(String.format("[DEBUG] Validated bed head at %s (type: %s)", 
                        formatLocation(block.getLocation()), material.name()));
                }
                return isHead;
            }
        } catch (Exception e) {
            // Fallback for older versions or edge cases
            if (instance != null && instance.debugLogging) {
                Specialization.logger.warning(String.format("[DEBUG] Bed validation failed for %s: %s", 
                    formatLocation(block.getLocation()), e.getMessage()));
            }
        }
        
        return false;
    }

    static Location calculateTownCenter(List<Location> beds) {
        if (beds.isEmpty()) return null;

        double totalX = 0, totalY = 0, totalZ = 0;
        World world = beds.getFirst().getWorld();

        for (Location bed : beds) {
            totalX += bed.getX();
            totalY += bed.getY();
            totalZ += bed.getZ();
        }

        return new Location(world,
                totalX / beds.size(),
                totalY / beds.size(),
                totalZ / beds.size());
    }

    private static void updateTownsList() {
        synchronized (towns) {
            for (int i = 0; i < towns.size(); i++) {
                for (int j = i + 1; j < towns.size(); j++) {
                    Town town1 = towns.get(i);
                    Town town2 = towns.get(j);

                    if (town1.getCenterLocation().getWorld().equals(town2.getCenterLocation().getWorld()) &&
                            town1.getCenterLocation().distance(town2.getCenterLocation()) <= TOWN_RADIUS) {

                        if (town1.getBedCount() >= town2.getBedCount()) {
                            towns.remove(j);
                            j--;
                        } else {
                            towns.remove(i);
                            i--;
                            break;
                        }
                    }
                }
            }
        }
    }

    public void setDebugLogging(boolean enabled) {
        this.debugLogging = enabled;
        if (enabled) {
            Specialization.logger.info("Town detection debug logging enabled");
        } else {
            Specialization.logger.info("Town detection debug logging disabled");
        }
    }

    public void getDetectionStats() {
        Specialization.logger.info("=== Town Detection Statistics ===");
        Specialization.logger.info("Total towns discovered: " + towns.size());
        Specialization.logger.info("Players tracked: " + playerSpawnLocations.size());
        Specialization.logger.info("Periodic scanning: " + (periodicScanTask != null ? "Enabled" : "Disabled"));
        Specialization.logger.info("Debug logging: " + (debugLogging ? "Enabled" : "Disabled"));

        synchronized (towns) {
            List<Town> recentTowns = towns.stream()
                    .sorted((t1, t2) -> Long.compare(t2.getDiscoveredTime(), t1.getDiscoveredTime()))
                    .limit(5)
                    .toList();

            Specialization.logger.info("Recent towns discovered:");
            for (int i = 0; i < recentTowns.size(); i++) {
                Town town = recentTowns.get(i);
                Specialization.logger.info(String.format("  %d. %s (%d beds)",
                        i + 1, formatLocation(town.getCenterLocation()), town.getBedCount()));
            }
        }
    }

    public void cleanup() {
        stopPeriodicScanning();
        lastScanLocations.clear();
    }

    public static List<Town> getTowns() {
        return new ArrayList<>(towns);
    }

    public static Map<UUID, Location> getPlayerSpawnLocations() {
        return new HashMap<>(playerSpawnLocations);
    }

    public static String formatLocation(Location loc) {
        return String.format("%.1f, %.1f, %.1f in %s",
                loc.getX(), loc.getY(), loc.getZ(), loc.getWorld().getName());
    }
}