package com.minecraftcivilizations.specialization.Distance;

import com.minecraftcivilizations.specialization.Specialization;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.CraftChunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class TownManager implements Listener {
    @Getter
    private static TownManager instance;
    private static final List<Town> towns = Collections.synchronizedList(new ArrayList<>());
    private static final Map<UUID, Location> playerSpawnLocations = new ConcurrentHashMap<>();
    private static final int TOWN_RADIUS = 150;
    private static final int MIN_BEDS = 5;

    public TownManager() {
        instance = this;
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Location spawnLocation = event.getRespawnLocation();
        UUID playerId = event.getPlayer().getUniqueId();

        playerSpawnLocations.put(playerId, spawnLocation);

        // Async scan around respawn
        CompletableFuture.runAsync(() -> scanForTownsAroundLocationAsync(spawnLocation));
    }

    /*** ASYNC STARTUP SCAN ***/
    public static void scanAllPlayersForTownsAsync() {
        Specialization.logger.info("Starting town scan...");
        long startTime = System.currentTimeMillis();

        CompletableFuture.runAsync(() -> {
            List<Town> tempTowns = new ArrayList<>();
            Set<Location> scannedLocations = new HashSet<>();

            for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                Location bedLoc = player.getRespawnLocation();
                if (bedLoc == null) continue;

                playerSpawnLocations.put(player.getUniqueId(), bedLoc);

                boolean alreadyScanned = scannedLocations.stream()
                        .anyMatch(loc -> loc.getWorld() == bedLoc.getWorld() &&
                                loc.distanceSquared(bedLoc) <= TOWN_RADIUS * TOWN_RADIUS);
                if (alreadyScanned) continue;

                scannedLocations.add(bedLoc);

                // Scan beds around bedLoc on main thread
                List<Location> bedsInArea = new ArrayList<>();
                CompletableFuture<Void> scanTask = new CompletableFuture<>();
                Bukkit.getScheduler().runTask(Specialization.getInstance(), () -> {
                    bedsInArea.addAll(findBedsInRadius(bedLoc));
                    scanTask.complete(null);
                });
                scanTask.join();

                if (bedsInArea.size() >= MIN_BEDS) {
                    Location center = calculateTownCenter(bedsInArea);
                    tempTowns.add(new Town(center, bedsInArea));
                }
            }

            // Merge towns that are too close
            mergeCloseTowns(tempTowns);

            // Push to main thread
            Bukkit.getScheduler().runTask(Specialization.getInstance(), () -> {
                towns.clear();
                towns.addAll(tempTowns);

                long duration = System.currentTimeMillis() - startTime;
                double durationSeconds = duration / 1000.0;
                Specialization.logger.info("Town scan complete! Found " + towns.size() + " towns in " + durationSeconds + "s");

                for (int i = 0; i < towns.size(); i++) {
                    Town t = towns.get(i);
                    Specialization.logger.info("Town " + (i + 1) + ": " + t.getBedCount() +
                            " beds at " + formatLocation(t.getCenterLocation()));
                }
            });
        });
    }

    /*** ASYNC SCAN AROUND A SPECIFIC LOCATION ***/
    private static void scanForTownsAroundLocationAsync(Location centerLocation) {
        CompletableFuture.runAsync(() -> {
            List<Location> bedsInArea = new ArrayList<>();
            CompletableFuture<Void> scanTask = new CompletableFuture<>();
            Bukkit.getScheduler().runTask(Specialization.getInstance(), () -> {
                bedsInArea.addAll(findBedsInRadius(centerLocation));
                scanTask.complete(null);
            });
            scanTask.join();

            if (bedsInArea.size() >= MIN_BEDS) {
                synchronized (towns) {
                    boolean townExists = false;
                    for (Town existingTown : towns) {
                        if (existingTown.getCenterLocation().getWorld().equals(centerLocation.getWorld()) &&
                                existingTown.getCenterLocation().distanceSquared(centerLocation) <= TOWN_RADIUS * TOWN_RADIUS) {
                            if (bedsInArea.size() > existingTown.getBedCount()) {
                                existingTown.updateBeds(bedsInArea);
                            }
                            townExists = true;
                            break;
                        }
                    }
                    if (!townExists) {
                        towns.add(new Town(Objects.requireNonNull(calculateTownCenter(bedsInArea)), bedsInArea));
                    }
                }
            }
        });
    }

    /*** MERGE CLOSE TOWNS ***/
    private static void mergeCloseTowns(List<Town> townList) {
        for (int i = 0; i < townList.size(); i++) {
            Town t1 = townList.get(i);
            for (int j = i + 1; j < townList.size(); j++) {
                Town t2 = townList.get(j);
                if (t1.getCenterLocation().getWorld().equals(t2.getCenterLocation().getWorld()) &&
                        t1.getCenterLocation().distanceSquared(t2.getCenterLocation()) <= TOWN_RADIUS * TOWN_RADIUS) {
                    if (t1.getBedCount() >= t2.getBedCount()) {
                        townList.remove(j);
                    } else {
                        townList.remove(i);
                        i--;
                    }
                    break;
                }
            }
        }
    }

    private static List<Location> findBedsInRadius(Location center) {
        List<Location> beds = new ArrayList<>();
        World world = center.getWorld();
        if (world == null) return beds;

        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();

        int minX = centerX - TOWN_RADIUS;
        int maxX = centerX + TOWN_RADIUS;
        int minZ = centerZ - TOWN_RADIUS;
        int maxZ = centerZ + TOWN_RADIUS;

        int minChunkX = minX >> 4;
        int maxChunkX = maxX >> 4;
        int minChunkZ = minZ >> 4;
        int maxChunkZ = maxZ >> 4;

        int operations = 0;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                Chunk chunk = world.getChunkAt(chunkX, chunkZ);

                // Iterate all tile entities in the chunk
                for (BlockState state : ((org.bukkit.craftbukkit.CraftChunk) chunk).getTileEntities()) {
                    operations++;

                    // Check if this BlockState is a bed
                    Material type = state.getType();
                    if (type.name().endsWith("BED")) {
                        Location bedLoc = state.getLocation();

                        // Only include if within spherical radius
                        if (center.distanceSquared(bedLoc) <= TOWN_RADIUS * TOWN_RADIUS) {
                            beds.add(bedLoc);
                        }
                    }
                }
            }
        }

        Specialization.logger.info("findBedsInRadius completed. Checked " + operations + " tile entities.");

        return beds;
    }



    private static boolean isBed(Block block) {
        Material material = block.getType();
        if (!material.name().endsWith("BED")) return false;

        try {
            if (block.getBlockData() instanceof org.bukkit.block.data.type.Bed bed) {
                return bed.getPart() == org.bukkit.block.data.type.Bed.Part.HEAD ||
                        bed.getPart() == org.bukkit.block.data.type.Bed.Part.FOOT;
            }
        } catch (Exception e) {
            return true;
        }

        return false;
    }

    private static Location calculateTownCenter(List<Location> beds) {
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
