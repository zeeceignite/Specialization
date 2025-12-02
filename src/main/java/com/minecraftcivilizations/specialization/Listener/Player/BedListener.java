package com.minecraftcivilizations.specialization.Listener.Player;

import com.minecraftcivilizations.specialization.Specialization;
import com.minecraftcivilizations.specialization.util.EffectsUtil;
import com.minecraftcivilizations.specialization.util.PlayerUtil;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Campfire;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;


public class BedListener implements Listener {

    private static final NamespacedKey BED_OWNER_KEY = new NamespacedKey(Specialization.getInstance(), "bed_owner");
    private static final NamespacedKey PLAYER_BED_ID = new NamespacedKey(Specialization.getInstance(), "bed_id");
    private static final NamespacedKey PLAYER_BED_X = new NamespacedKey(Specialization.getInstance(), "bed_x");
    private static final NamespacedKey PLAYER_BED_Y = new NamespacedKey(Specialization.getInstance(), "bed_y");
    private static final NamespacedKey PLAYER_BED_Z = new NamespacedKey(Specialization.getInstance(), "bed_z");

    private static boolean isBed(Material material) {
        return material.name().endsWith("_BED");
    }

    private static Block getBedHeadBlock(Block block) {
        if (!isBed(block.getType())) return null;
        BlockData data = block.getBlockData();
        if (!(data instanceof Bed bed)) return null;
        return bed.getPart() == Bed.Part.HEAD ? block : block.getRelative(bed.getFacing());
    }

    private static String getBedId(Block headBlock) {
        if (!(headBlock.getState() instanceof TileState state)) return null;
        return state.getPersistentDataContainer().get(BED_OWNER_KEY, PersistentDataType.STRING);
    }

    private static void setBedId(Block headBlock, String uuid) {
        if (!(headBlock.getState() instanceof TileState state)) return;
        state.getPersistentDataContainer().set(BED_OWNER_KEY, PersistentDataType.STRING, uuid);
        state.update(true);
    }

    private static void clearBedId(Block headBlock) {
        if (!(headBlock.getState() instanceof TileState state)) return;
        state.getPersistentDataContainer().remove(BED_OWNER_KEY);
        state.update(true);
    }

    private static String getPlayerBedId(Player player) {
        return player.getPersistentDataContainer().get(PLAYER_BED_ID, PersistentDataType.STRING);
    }

    private static void clearPlayerBed(Player player) {
        player.getPersistentDataContainer().remove(PLAYER_BED_ID);
        player.getPersistentDataContainer().remove(PLAYER_BED_X);
        player.getPersistentDataContainer().remove(PLAYER_BED_Y);
        player.getPersistentDataContainer().remove(PLAYER_BED_Z);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractWithBed(PlayerBedEnterEvent event) {
        Player player = event.getPlayer();
        Block clickedBlock = event.getBed();

        switch (event.getBedEnterResult()) {
            case OK:
            case NOT_POSSIBLE_NOW:
            case NOT_SAFE:
                break;
            default:
                return;
        }

        Block headBlock = getBedHeadBlock(clickedBlock);
        if (headBlock == null) return;

        String bedOwnerUUID = getBedId(headBlock);

        // --- 1. DENY OTHER PLAYERS FIRST ---
        if (bedOwnerUUID != null && !bedOwnerUUID.equals(player.getUniqueId().toString())) {
            event.setCancelled(true);
            PlayerUtil.message(player, "§6This bed is already claimed by another player");
            return;
        }

        // --- 2. UNCLAIM WHEN SHIFTING (ONLY OWNER) ---
        if (player.isSneaking() && Objects.equals(bedOwnerUUID, player.getUniqueId().toString())) {
            clearBedId(headBlock);
            clearPlayerBed(player);
            player.setRespawnLocation(null, true);

            float pitch = (float) ThreadLocalRandom.current().nextDouble(0.3, 0.6);
            clickedBlock.getWorld().playSound(clickedBlock.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_PLACE, 10f, pitch);
            PlayerUtil.message(player,"§6You have unclaimed your bed");
            return;
        }

        // --- 3. CLAIM IF UNOWNED ---
        if (bedOwnerUUID == null) {
            clearOldBed(player);

            String uuidStr = player.getUniqueId().toString();
            setBedId(headBlock, uuidStr);
            player.getPersistentDataContainer().set(PLAYER_BED_ID, PersistentDataType.STRING, uuidStr);
            player.getPersistentDataContainer().set(PLAYER_BED_X, PersistentDataType.INTEGER, headBlock.getX());
            player.getPersistentDataContainer().set(PLAYER_BED_Y, PersistentDataType.INTEGER, headBlock.getY());
            player.getPersistentDataContainer().set(PLAYER_BED_Z, PersistentDataType.INTEGER, headBlock.getZ());

            EffectsUtil.playBlockBoundingBox(player, clickedBlock, Particle.HAPPY_VILLAGER, 0.25);
            float pitch = (float) ThreadLocalRandom.current().nextDouble(0.9, 1.3);
            clickedBlock.getWorld().playSound(clickedBlock.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 10f, pitch);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || !(block.getBlockData() instanceof Bed)) return;

        Player player = event.getPlayer();

        Block headBlock = getBedHeadBlock(block);
        if (headBlock == null) return;

        String bedOwnerUUID = getBedId(headBlock);

        // --- CANCEL IF CLAIMED BY SOMEONE ELSE ---
        if (bedOwnerUUID != null && !bedOwnerUUID.equals(player.getUniqueId().toString())) {
            PlayerUtil.message(player,"§7This bed is already claimed by another player");
            event.setCancelled(true);
            return;
        }
    }

    // --- BED BREAK ---
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBedBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!isBed(block.getType())) return;

        Block headBlock = getBedHeadBlock(block);
        if (headBlock == null) return;

        /*
            ADDED specifically for debugging CombatSystem
         */
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE) {
            if (event.getPlayer().getEquipment().getItemInMainHand().getType().name().contains("SWORD")) {
                return;
            }
        }

        String bedOwnerUUIDStr = getBedId(headBlock);
        if (bedOwnerUUIDStr != null) {
            clearBedId(headBlock);

            // Directly get player by UUID
            UUID ownerUUID = UUID.fromString(bedOwnerUUIDStr);
            Player owner = Bukkit.getPlayer(ownerUUID);
            if (owner != null) {
                clearPlayerBed(owner);
                owner.setRespawnLocation(null, true);
                PlayerUtil.message(owner,"§cYour bed was destroyed");
            }
        }
    }

    // --- PLAYER RESPAWN ---
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        String bedId = player.getPersistentDataContainer().get(PLAYER_BED_ID, PersistentDataType.STRING);

        if (bedId == null) return; // vanilla handles spawn naturally

        Integer x = player.getPersistentDataContainer().get(PLAYER_BED_X, PersistentDataType.INTEGER);
        Integer y = player.getPersistentDataContainer().get(PLAYER_BED_Y, PersistentDataType.INTEGER);
        Integer z = player.getPersistentDataContainer().get(PLAYER_BED_Z, PersistentDataType.INTEGER);

        // If the bed no longer exists, clear the player data
        if (x == null || y == null || z == null) {
            clearPlayerBed(player);
            return;
        }

        Block bedBlock = player.getWorld().getBlockAt(x, y, z);
        Block headBlock = getBedHeadBlock(bedBlock);

        if (headBlock == null || !bedId.equals(getBedId(headBlock))) {
            clearPlayerBed(player);
        }
    }

    private void clearOldBed(Player player) {
        Integer oldX = player.getPersistentDataContainer().get(PLAYER_BED_X, PersistentDataType.INTEGER);
        Integer oldY = player.getPersistentDataContainer().get(PLAYER_BED_Y, PersistentDataType.INTEGER);
        Integer oldZ = player.getPersistentDataContainer().get(PLAYER_BED_Z, PersistentDataType.INTEGER);

        if (oldX != null && oldY != null && oldZ != null) {
            player.getWorld().getChunkAt(oldX, oldZ).load(true);
            Block oldBedBlock = player.getWorld().getBlockAt(oldX, oldY, oldZ);
            Block oldHead = getBedHeadBlock(oldBedBlock);
            if (oldHead != null) clearBedId(oldHead);
        }
        clearPlayerBed(player);
    }

    //-----------BED HEALING ---------------------//
    private static final double NIGHT_HEAL_CAP = 5.0; // 2.5 hearts
    private static final long BED_HEAL_INTERVAL = 1200L; // ticks between heals
    private final Map<UUID, Long> lastHealDay = new HashMap<>();
    private final Map<UUID, Double> healedThisDay = new HashMap<>();
    private final Map<UUID, BedHealingTasks> bedHealTasks = new HashMap<>();


    private void startBedHealing(Player player) {
        UUID id = player.getUniqueId();
        if (bedHealTasks.containsKey(id)) return;

        // --- Check for warm campfire ---
        if (!hasWarmCampfire(player.getLocation())) {
            PlayerUtil.message(player, "<gray>Bed is too <aqua>Cold</aqua> to recover health</gray>");
            stopBedHealing(player);
            return;
        }

        long currentDay = player.getWorld().getFullTime() / 24000L;

        long storedDay = lastHealDay.getOrDefault(id, -1L);
        final double[] used = {healedThisDay.getOrDefault(id, 0.0)};

        // new day → reset
        if (storedDay != currentDay) {
            lastHealDay.put(id, currentDay);
            healedThisDay.put(id, 0.0);
            used[0] = 0.0;
        }

        // enforce cap
        if (used[0] >= NIGHT_HEAL_CAP) {
            PlayerUtil.message(player, "<gray>You're fully rested</gray>");
            stopBedHealing(player);
            return;
        }

        BedHealingTasks tasks = new BedHealingTasks();

        // --- Create BossBar ---
        BossBar bar = Bukkit.createBossBar("Resting", BarColor.GREEN, BarStyle.SEGMENTED_20);
        bar.addPlayer(player);
        bar.setProgress(0);
        tasks.bossBar = bar;

        PlayerUtil.message(player, "<gray>You feel <gold>warm</gold> and cozy enough to <green>recover</green> health");
        final int totalTicks = (int) BED_HEAL_INTERVAL;
        final int[] tickCounter = {0};

        // --- BossBar + Healing updater ---
        tasks.bossBarUpdater = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isSleeping()) {
                    stopBedHealing(player);
                    return;
                }

                tickCounter[0]++;
                double progress = Math.min((double) tickCounter[0] / totalTicks, 1.0);
                bar.setProgress(progress);

                //--------------start increment---------------
                if (progress < 1.0) return;

                // enforce cap
                if (used[0] >= NIGHT_HEAL_CAP) {
                    PlayerUtil.message(player, "<gray>You're fully rested</gray>");
                    stopBedHealing(player);
                    return;
                }

                // --- Apply healing ---
                double max = player.getAttribute(Attribute.MAX_HEALTH).getValue();
                double newHealth = Math.min(player.getHealth() + 1, max);
                player.setHealth(newHealth);

                // --- increment healed amount this night ---
                used[0] += 1.0;
                healedThisDay.put(id, used[0]);

                // hit max → end healing
                if (newHealth >= max) {
                    stopBedHealing(player);
                    return;
                }
                // --- Check for warm campfire ---
                if (!hasWarmCampfire(player.getLocation())) {
                    PlayerUtil.message(player, "<gray>Bed is too <aqua>Cold</aqua> to recover health</gray>");
                    stopBedHealing(player);
                    return;
                }

                // --- HEART PARTICLES ---
                player.getWorld().spawnParticle(
                        Particle.HEART,
                        player.getLocation().add(0, 1.0, 0),
                        1,
                        0.3, 0.3, 0.3,
                        0
                );

                player.getWorld().playSound(
                        player.getLocation(),
                        Sound.BLOCK_AMETHYST_BLOCK_CHIME,
                        SoundCategory.PLAYERS,
                        2, 1
                );

                // --- TEXT HEART BAR ---
                double health = newHealth;
                int full = (int) (health / 2);
                boolean half = (health % 2) == 1;
                int total = (int) (max / 2);

                StringBuilder barText = new StringBuilder();

                for (int i = 0; i < full; i++)
                    barText.append("<dark_red>❤</dark_red>");

                if (half)
                    barText.append("<#804040>❤</#804040>");

                int empty = total - full - (half ? 1 : 0);
                for (int i = 0; i < empty; i++)
                    barText.append("<#383838>❤</#383838>");

                PlayerUtil.message(player, barText.toString());

                // reset bar for next heal cycle
                tickCounter[0] = 0;
            }
        };

        tasks.bossBarUpdater.runTaskTimer(Specialization.getInstance(), 0L, 1L);
        bedHealTasks.put(id, tasks);
    }

    private void stopBedHealing(Player player) {
        UUID id = player.getUniqueId();
        BedHealingTasks tasks = bedHealTasks.remove(id);

        if (tasks != null) {
            if (tasks.bossBarUpdater != null)
                tasks.bossBarUpdater.cancel();

            if (tasks.bossBar != null)
                tasks.bossBar.removeAll();
        }
    }

    private boolean hasWarmCampfire(Location bedLoc) {
        World w = bedLoc.getWorld();
        int bx = bedLoc.getBlockX();
        int by = bedLoc.getBlockY();
        int bz = bedLoc.getBlockZ();

        for (int x = -4; x <= 4; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -4; z <= 4; z++) {

                    Block b = w.getBlockAt(bx + x, by + y, bz + z);
                    BlockData data = b.getBlockData();

                    if (data instanceof Campfire lit && lit.isLit()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBedEnter(PlayerBedEnterEvent event) {
        if (event.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.OK) return;
        startBedHealing(event.getPlayer());
    }

    @EventHandler
    public void onSleepingFoodLoss(FoodLevelChangeEvent e) {
        if (e.getEntity() instanceof Player p) {
            if (p.isSleeping()) {
                e.setCancelled(true);
            }
        }
    }


    @EventHandler(priority = EventPriority.MONITOR)
    public void onBedLeave(org.bukkit.event.player.PlayerBedLeaveEvent event) {
        stopBedHealing(event.getPlayer());
    }

    private static class BedHealingTasks {
        BossBar bossBar;
        BukkitRunnable bossBarUpdater;
    }

}