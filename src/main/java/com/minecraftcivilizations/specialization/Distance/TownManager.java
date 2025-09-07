package com.minecraftcivilizations.specialization.Distance;

import com.minecraftcivilizations.specialization.Specialization;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class TownManager implements Listener {
    @Getter
    public static TownManager instance;
    private static final List<Town> towns = Collections.synchronizedList(new ArrayList<>());
    private static final Map<UUID, Location> playerSpawnLocations = new ConcurrentHashMap<>();
    private static final int TOWN_RADIUS = 150;
    private static final int MIN_BEDS = 4;

    public TownManager() {
        instance = this;
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Location spawnLocation = event.getRespawnLocation();
        UUID playerId = event.getPlayer().getUniqueId();

        // Store player spawn location
        playerSpawnLocations.put(playerId, spawnLocation);
        Specialization.logger.info("Player " + event.getPlayer().getName() + " respawned at " + formatLocation(spawnLocation));

        // Scan for towns around this spawn location
        new BukkitRunnable() {
            @Override
            public void run() {
                scanForTownsAroundLocation(spawnLocation);
                updateTownsList();
            }
        }.runTaskAsynchronously(Specialization.getInstance());
    }

    public static void scanAllPlayersForTowns() {
        Specialization.logger.info("Starting comprehensive town scan...");

        // Clear existing towns for fresh scan
        towns.clear();
        playerSpawnLocations.clear();

        OfflinePlayer[] allPlayers = Bukkit.getOfflinePlayers();
        Set<Location> scannedLocations = new HashSet<>();
        int playersWithBeds = 0;
        int totalPlayers = allPlayers.length;

        Specialization.logger.info("Scanning " + totalPlayers + " players for spawn locations...");

        for (OfflinePlayer player : allPlayers) {
            Location spawnLocation = null;
            
            // Try multiple methods to get player spawn location
            if (player.getBedSpawnLocation() != null) {
                spawnLocation = player.getBedSpawnLocation();
                Specialization.logger.info("Player " + player.getName() + " has bed spawn at " + formatLocation(spawnLocation));
                playersWithBeds++;
            } else if (player instanceof Player onlinePlayer) {
                // For online players, try getRespawnLocation
                Location respawnLoc = onlinePlayer.getRespawnLocation();
                if (respawnLoc != null) {
                    spawnLocation = respawnLoc;
                    Specialization.logger.info("Player " + player.getName() + " has respawn location at " + formatLocation(spawnLocation));
                }
            }

            if (spawnLocation != null) {
                playerSpawnLocations.put(player.getUniqueId(), spawnLocation);

                // Only scan if we haven't scanned this area before
                if (!isLocationNearScanned(spawnLocation, scannedLocations, TOWN_RADIUS)) {
                    Specialization.logger.info("Scanning area around " + formatLocation(spawnLocation) + " for beds...");
                    scanForTownsAroundLocation(spawnLocation);
                    scannedLocations.add(spawnLocation);
                }
            }
        }

        updateTownsList();

        Specialization.logger.info("Scan summary: " + playersWithBeds + "/" + totalPlayers + " players have bed spawns");
        Specialization.logger.info("Scanned " + scannedLocations.size() + " unique areas");

        new BukkitRunnable() {
            @Override
            public void run() {
                Specialization.logger.info("=== TOWN SCAN COMPLETE ===");
                Specialization.logger.info("Found " + towns.size() + " towns:");
                for (int i = 0; i < towns.size(); i++) {
                    Town town = towns.get(i);
                    Specialization.logger.info("Town " + (i + 1) + ": " + town.getBedCount() +
                            " beds at " + formatLocation(town.getCenterLocation()));
                }
                if (towns.isEmpty()) {
                    Specialization.logger.warning("No towns detected! This might indicate an issue with bed detection.");
                }
                Specialization.logger.info("=========================");
            }
        }.runTask(Specialization.getInstance());
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

    private static void scanForTownsAroundLocation(Location centerLocation) {
        World world = centerLocation.getWorld();
        if (world == null) {
            Specialization.logger.warning("Cannot scan - world is null for location " + formatLocation(centerLocation));
            return;
        }

        Specialization.logger.info("Scanning for beds within " + TOWN_RADIUS + " blocks of " + formatLocation(centerLocation));
        
        List<Location> bedsFound = findBedsInRadius(centerLocation, TOWN_RADIUS);
        
        Specialization.logger.info("Found " + bedsFound.size() + " beds around " + formatLocation(centerLocation));

        if (bedsFound.size() >= MIN_BEDS) {
            // Check if this area already has a town
            boolean townExists = false;
            synchronized (towns) {
                for (Town existingTown : towns) {
                    if (existingTown.getCenterLocation().getWorld().equals(world) &&
                            existingTown.getCenterLocation().distance(centerLocation) <= TOWN_RADIUS) {
                        // Update existing town with new beds if found more
                        if (bedsFound.size() > existingTown.getBedCount()) {
                            existingTown.updateBeds(bedsFound);
                            Specialization.logger.info("Updated existing town with " + bedsFound.size() + " beds");
                        }
                        townExists = true;
                        break;
                    }
                }
            }

            if (!townExists) {
                Location townCenter = calculateTownCenter(bedsFound);
                Town newTown = new Town(townCenter, bedsFound);
                towns.add(newTown);
                Specialization.logger.info("*** CREATED NEW TOWN *** with " + bedsFound.size() + " beds at " + formatLocation(townCenter));
            }
        } else {
            Specialization.logger.info("Not enough beds (" + bedsFound.size() + "/" + MIN_BEDS + ") to create town around " + formatLocation(centerLocation));
        }
    }

    private static List<Location> findBedsInRadius(Location center, int radius) {
        List<Location> beds = new ArrayList<>();
        World world = center.getWorld();
        if (world == null) {
            Specialization.logger.warning("Cannot find beds - world is null");
            return beds;
        }

        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();
        
        int blocksScanned = 0;
        int bedsFound = 0;

        Specialization.logger.info("Starting bed scan in " + (radius * 2) + "x" + (radius * 2) + "x" + (radius * 2) + " area...");

        // Search in a cube around the center, then filter by actual distance
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                for (int y = Math.max(world.getMinHeight(), centerY - radius);
                     y <= Math.min(world.getMaxHeight(), centerY + radius); y++) {

                    blocksScanned++;
                    Location checkLoc = new Location(world, x, y, z);

                    // Check if within actual radius (sphere)
                    if (center.distance(checkLoc) <= radius) {
                        Block block = world.getBlockAt(x, y, z);
                        if (isBed(block)) {
                            beds.add(checkLoc);
                            bedsFound++;
                            if (bedsFound <= 10) { // Log first 10 beds found
                                Specialization.logger.info("Found bed #" + bedsFound + " at " + formatLocation(checkLoc) + " (Material: " + block.getType() + ")");
                            }
                        }
                    }
                }
            }
        }

        Specialization.logger.info("Bed scan complete: " + bedsFound + " beds found after scanning " + blocksScanned + " blocks");
        return beds;
    }

    private static boolean isBed(Block block) {
        if (block == null) return false;
        
        Material material = block.getType();
        boolean isBedMaterial = material.name().endsWith("_BED");
        
        if (isBedMaterial) {
            try {
                // Count ALL bed blocks (both head and foot parts)
                if (block.getBlockData() instanceof org.bukkit.block.data.type.Bed) {
                    return true;
                }
            } catch (Exception e) {
                // Fallback: if we can't determine the bed data, still count it if it's a bed material
                Specialization.logger.warning("Could not get bed data for " + material + " at " + formatLocation(block.getLocation()) + ", but counting as bed anyway");
                return true;
            }
        }
        
        return false;
    }

    static Location calculateTownCenter(List<Location> beds) {
        if (beds.isEmpty()) return null;

        double totalX = 0, totalY = 0, totalZ = 0;
        World world = beds.get(0).getWorld();

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
        // Remove duplicate towns that might be too close to each other
        for (int i = 0; i < towns.size(); i++) {
            for (int j = i + 1; j < towns.size(); j++) {
                Town town1 = towns.get(i);
                Town town2 = towns.get(j);

                if (town1.getCenterLocation().getWorld().equals(town2.getCenterLocation().getWorld()) &&
                        town1.getCenterLocation().distance(town2.getCenterLocation()) <= TOWN_RADIUS) {

                    // Merge towns - keep the one with more beds
                    if (town1.getBedCount() >= town2.getBedCount()) {
                        towns.remove(j);
                        Specialization.logger.info("Merged duplicate towns - kept town with " + town1.getBedCount() + " beds");
                    } else {
                        towns.remove(i);
                        Specialization.logger.info("Merged duplicate towns - kept town with " + town2.getBedCount() + " beds");
                        i--; // Adjust index after removal
                    }
                    break;
                }
            }
        }
    }

    public static List<Town> getTowns() {
        return new ArrayList<>(towns);
    }

    public static Map<UUID, Location> getPlayerSpawnLocations() {
        return new HashMap<>(playerSpawnLocations);
    }

    public static String formatLocation(Location loc) {
        if (loc == null) return "null";
        return String.format("%.1f, %.1f, %.1f in %s",
                loc.getX(), loc.getY(), loc.getZ(), loc.getWorld().getName());
    }
}
