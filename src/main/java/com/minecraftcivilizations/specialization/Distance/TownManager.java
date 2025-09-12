package com.minecraftcivilizations.specialization.Distance;

import com.minecraftcivilizations.specialization.Specialization;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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
        Location spawnLocation = event.getRespawnLocation();
        UUID playerId = event.getPlayer().getUniqueId();

        playerSpawnLocations.put(playerId, spawnLocation);

        new BukkitRunnable() {
            @Override
            public void run() {
                scanForTownsAroundLocation(spawnLocation, "Player Respawn: " + event.getPlayer().getName());
                updateTownsList();
                getDetectionStats();
            }
        }.runTaskAsynchronously(Specialization.getInstance());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Location loc = event.getPlayer().getLocation();
        UUID playerId = event.getPlayer().getUniqueId();

        new BukkitRunnable() {
            @Override
            public void run() {
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
        Specialization.logger.info("Starting comprehensive town scan...");

        towns.clear();

        OfflinePlayer[] allPlayers = Bukkit.getOfflinePlayers();
        Set<Location> scannedLocations = new HashSet<>();

        for (OfflinePlayer player : allPlayers) {
            if (player.getBedSpawnLocation() != null) {
                Location bedLocation = player.getBedSpawnLocation();
                playerSpawnLocations.put(player.getUniqueId(), bedLocation);

                if (!isLocationNearScanned(bedLocation, scannedLocations, TOWN_RADIUS / SCAN_RADIUS_REDUCTION)) {
                    scanForTownsAroundLocation(bedLocation, "Comprehensive Scan: " + player.getName());
                    scannedLocations.add(bedLocation);
                }
            }
        }

        updateTownsList();
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

        List<Location> bedsInArea = findBedsInRadius(centerLocation, TOWN_RADIUS);

        if (instance.debugLogging && bedsInArea.size() > 0) {
            Specialization.logger.info(String.format("[%s] Scanning %s: found %d beds",
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
                            if (instance.debugLogging) {
                                Specialization.logger.info(String.format("[%s] Updated existing town at %s with %d beds",
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

                if (instance.debugLogging) {
                    Specialization.logger.info(String.format("[%s] Created new town at %s with %d beds",
                            source, formatLocation(townCenter), bedsInArea.size()));
                }
            }
        }
    }

    private static void scanForTownsAroundLocation(Location centerLocation) {
        scanForTownsAroundLocation(centerLocation, "Legacy Scan");
    }

    private static List<Location> findBedsInRadius(Location center, int radius) {
        List<Location> beds = new ArrayList<>();
        World world = center.getWorld();
        if (world == null) return beds;

        int chunkRadius = (radius / 16) + 1;
        int centerChunkX = center.getBlockX() >> 4;
        int centerChunkZ = center.getBlockZ() >> 4;

        for (int chunkX = centerChunkX - chunkRadius; chunkX <= centerChunkX + chunkRadius; chunkX++) {
            for (int chunkZ = centerChunkZ - chunkRadius; chunkZ <= centerChunkZ + chunkRadius; chunkZ++) {

                if (!world.isChunkLoaded(chunkX, chunkZ)) {
                    continue;
                }

                Chunk chunk = world.getChunkAt(chunkX, chunkZ);

                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                            Block block = chunk.getBlock(x, y, z);
                            Location blockLoc = block.getLocation();

                            if (center.distance(blockLoc) <= radius && isBed(block)) {
                                beds.add(blockLoc);
                            }
                        }
                    }
                }
            }
        }

        return beds;
    }

    private static boolean isBed(Block block) {
        Material material = block.getType();
        if (!material.name().endsWith("BED")) return false;
        try {
            if (block.getBlockData() instanceof org.bukkit.block.data.type.Bed bed) {
                return bed.getPart() == org.bukkit.block.data.type.Bed.Part.HEAD;
            }
        } catch (Exception e) {
            return (block.getX() + block.getY() + block.getZ()) % 2 == 0;
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