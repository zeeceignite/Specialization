package com.minecraftcivilizations.specialization.Distance;

import com.minecraftcivilizations.specialization.Specialization;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.persistence.PersistentDataContainer;
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
        Specialization.logger.info("Starting town scan...");

        // Clear existing towns for fresh scan
        towns.clear();

        OfflinePlayer[] allPlayers = Bukkit.getOfflinePlayers();
        Set<Location> scannedLocations = new HashSet<>();

        for (OfflinePlayer player : allPlayers) {
            if (player.getBedSpawnLocation() != null) {
                Location bedLocation = player.getBedSpawnLocation();
                playerSpawnLocations.put(player.getUniqueId(), bedLocation);

                // Only scan if we haven't scanned this area before
                if (!isLocationNearScanned(bedLocation, scannedLocations, TOWN_RADIUS)) {
                    scanForTownsAroundLocation(bedLocation);
                    scannedLocations.add(bedLocation);
                }
            }
        }

        updateTownsList();

        new BukkitRunnable() {
            @Override
            public void run() {
                Specialization.logger.info("Town scan complete! Found " + towns.size() + " towns:");
                for (int i = 0; i < towns.size(); i++) {
                    Town town = towns.get(i);
                    Specialization.logger.info("Town " + (i + 1) + ": " + town.getBedCount() +
                            " beds at " + formatLocation(town.getCenterLocation()));
                }
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
        if (world == null) return;

        // Use two-tier bed detection system
        List<Location> claimedBeds = getClaimedBedsInRadius(centerLocation, TOWN_RADIUS);
        List<Location> unclaimedBeds = findUnclaimedBedsInRadius(centerLocation, TOWN_RADIUS, claimedBeds);
        
        // Combine both lists for total bed count
        List<Location> allBeds = new ArrayList<>(claimedBeds);
        allBeds.addAll(unclaimedBeds);

        Specialization.logger.info("Found " + claimedBeds.size() + " claimed beds and " + 
                                 unclaimedBeds.size() + " unclaimed beds around " + formatLocation(centerLocation));

        if (allBeds.size() >= MIN_BEDS) {
            // Check if this area already has a town
            boolean townExists = false;
            synchronized (towns) {
                for (Town existingTown : towns) {
                    if (existingTown.getCenterLocation().getWorld().equals(world) &&
                            existingTown.getCenterLocation().distance(centerLocation) <= TOWN_RADIUS) {
                        // Update existing town with new beds if found more
                        if (allBeds.size() > existingTown.getBedCount()) {
                            existingTown.updateBeds(allBeds);
                            Specialization.logger.info("Updated existing town with " + allBeds.size() + " beds");
                        }
                        townExists = true;
                        break;
                    }
                }
            }

            if (!townExists) {
                Location townCenter = calculateTownCenter(allBeds);
                Town newTown = new Town(townCenter, allBeds);
                towns.add(newTown);
                Specialization.logger.info("Created new town with " + allBeds.size() + " beds at " + formatLocation(townCenter));
            }
        } else {
            Specialization.logger.info("Not enough beds (" + allBeds.size() + "/" + MIN_BEDS + ") to create town around " + formatLocation(centerLocation));
        }
    }

    /**
     * Priority 1: Get claimed beds from PDC system within radius
     */
    private static List<Location> getClaimedBedsInRadius(Location center, int radius) {
        List<Location> claimedBeds = new ArrayList<>();
        World world = center.getWorld();
        if (world == null) return claimedBeds;

        int centerChunkX = center.getBlockX() >> 4;
        int centerChunkZ = center.getBlockZ() >> 4;
        int chunkRadius = (radius >> 4) + 1; // Convert block radius to chunk radius

        for (int chunkX = centerChunkX - chunkRadius; chunkX <= centerChunkX + chunkRadius; chunkX++) {
            for (int chunkZ = centerChunkZ - chunkRadius; chunkZ <= centerChunkZ + chunkRadius; chunkZ++) {
                try {
                    org.bukkit.Chunk chunk = world.getChunkAt(chunkX, chunkZ);
                    PersistentDataContainer chunkPDC = chunk.getPersistentDataContainer();

                    // Scan for bed ownership keys
                    for (NamespacedKey key : chunkPDC.getKeys()) {
                        if (key.getNamespace().equals(Specialization.getInstance().getName()) &&
                            key.getKey().startsWith("bed_")) {
                            
                            // Parse location from key: "bed_x_y_z"
                            String[] parts = key.getKey().split("_");
                            if (parts.length == 4) {
                                try {
                                    int x = Integer.parseInt(parts[1]);
                                    int y = Integer.parseInt(parts[2]);
                                    int z = Integer.parseInt(parts[3]);
                                    
                                    Location bedLoc = new Location(world, x, y, z);
                                    
                                    // Check if within radius and bed still exists
                                    if (center.distance(bedLoc) <= radius) {
                                        Block bedBlock = world.getBlockAt(x, y, z);
                                        if (isBed(bedBlock)) {
                                            claimedBeds.add(bedLoc);
                                        } else {
                                            // Clean up stale PDC data
                                            chunkPDC.remove(key);
                                        }
                                    }
                                } catch (NumberFormatException e) {
                                    // Invalid key format, skip
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // Skip problematic chunks
                }
            }
        }

        return claimedBeds;
    }

    /**
     * Priority 2: Find unclaimed beds using raw block detection, excluding already found claimed beds
     */
    private static List<Location> findUnclaimedBedsInRadius(Location center, int radius, List<Location> claimedBeds) {
        List<Location> unclaimedBeds = new ArrayList<>();
        World world = center.getWorld();
        if (world == null) return unclaimedBeds;

        // Convert claimed beds to a HashSet for fast lookup
        Set<String> claimedBedKeys = new HashSet<>();
        for (Location claimedBed : claimedBeds) {
            claimedBedKeys.add(claimedBed.getBlockX() + "_" + claimedBed.getBlockY() + "_" + claimedBed.getBlockZ());
        }

        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();

        // Search in a cube around the center, then filter by actual distance
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                for (int y = Math.max(world.getMinHeight(), centerY - radius);
                     y <= Math.min(world.getMaxHeight(), centerY + radius); y++) {

                    Location checkLoc = new Location(world, x, y, z);

                    // Check if within actual radius (sphere)
                    if (center.distance(checkLoc) <= radius) {
                        // Skip if this bed is already in claimed beds list
                        String locationKey = x + "_" + y + "_" + z;
                        if (!claimedBedKeys.contains(locationKey)) {
                            Block block = world.getBlockAt(x, y, z);
                            if (isBed(block)) {
                                unclaimedBeds.add(checkLoc);
                            }
                        }
                    }
                }
            }
        }

        return unclaimedBeds;
    }

    private static List<Location> findBedsInRadius(Location center, int radius) {
        // This method is kept for backward compatibility but now uses the two-tier system
        List<Location> claimedBeds = getClaimedBedsInRadius(center, radius);
        List<Location> unclaimedBeds = findUnclaimedBedsInRadius(center, radius, claimedBeds);
        
        List<Location> allBeds = new ArrayList<>(claimedBeds);
        allBeds.addAll(unclaimedBeds);
        return allBeds;
    }

    private static boolean isBed(Block block) {
        Material material = block.getType();
        if (!material.name().endsWith("BED")) return false;
        try {
            if (block.getBlockData() instanceof org.bukkit.block.data.type.Bed bed) {
                return bed.getPart() == org.bukkit.block.data.type.Bed.Part.HEAD;
            }
        } catch (Exception e) {
            // Fallback: if we can't determine the part, count every other bed block
            // This is a safety measure for edge cases
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
                    } else {
                        towns.remove(i);
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
        return String.format("%.1f, %.1f, %.1f in %s",
                loc.getX(), loc.getY(), loc.getZ(), loc.getWorld().getName());
    }
}
