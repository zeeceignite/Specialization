package com.minecraftcivilizations.specialization.Combat;

import com.minecraftcivilizations.specialization.Listener.Player.PlayerDeathListener;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PVPManager implements Listener, CommandExecutor {

    private final JavaPlugin plugin;

    private final Map<UUID, Long> combatMap = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> zombieMap = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitRunnable> zombieTimers = new ConcurrentHashMap<>();
    private static final long COMBAT_COOLDOWN = 10_000L;
    private static final long ZOMBIE_LIFETIME = 15_000L; // 15s

    PlayerDeathListener deathListener = new PlayerDeathListener();
    private final NamespacedKey OWNER_KEY;
    private final NamespacedKey MARKER_KEY;
    private final NamespacedKey INVENTORY_KEY;
    private final NamespacedKey ARMOR_KEY;
    private final NamespacedKey HEALTH_KEY;
    private final NamespacedKey DEAD_KEY;

    public PVPManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.OWNER_KEY = new NamespacedKey(plugin, "owner");
        this.MARKER_KEY = new NamespacedKey(plugin, "pvp_marker");
        this.INVENTORY_KEY = new NamespacedKey(plugin, "inv");
        this.ARMOR_KEY = new NamespacedKey(plugin, "armor");
        this.HEALTH_KEY = new NamespacedKey(plugin, "health");
        this.DEAD_KEY = new NamespacedKey(plugin, "dead");

        Bukkit.getPluginManager().registerEvents(this, plugin);
        Objects.requireNonNull(plugin.getCommand("simulatehit")).setExecutor(this);
    }

    // --- Detect combat ---
    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Player damager = getDamager(event.getDamager());
        if (damager == null || damager.equals(victim)) return;

        long now = System.currentTimeMillis();

        // Check global combat state for both players
        boolean victimAlreadyTagged = combatMap.containsKey(victim.getUniqueId());
        boolean damagerAlreadyTagged = combatMap.containsKey(damager.getUniqueId());

        // Tag both
        combatMap.put(victim.getUniqueId(), now);
        combatMap.put(damager.getUniqueId(), now);

        // Only send messages if BOTH were NOT tagged before
        if (!victimAlreadyTagged)
            victim.sendMessage("§0[§0§6CivLabs§0]§8 » §7You have been tagged for §ccombat §7for §b" + (COMBAT_COOLDOWN / 1000) + " §7seconds by: §c" + damager.getName());

        if (!damagerAlreadyTagged)
            damager.sendMessage("§0[§0§6CivLabs§0]§8 » §7You are tagged for §ccombat §7for §b" + (COMBAT_COOLDOWN / 1000) + " §7seconds");


        plugin.getLogger().info("[Combat] " + damager.getName() + " hit " + victim.getName());

        // Reset zombie timer if hit
        UUID victimId = victim.getUniqueId();
        UUID zombieId = zombieMap.get(victimId);
        if (zombieId != null) {
            BukkitRunnable timer = zombieTimers.get(victimId);
            if (timer != null) {
                timer.cancel();
                startZombieTimer(victimId, Bukkit.getEntity(zombieId));
            }
        }
    }

    private Player getDamager(Entity source) {
        if (source instanceof Player p) return p;
        if (source instanceof Projectile proj && proj.getShooter() instanceof Player p) return p;
        return null;
    }

    // --- Player logout ---
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();

        Long lastHit = combatMap.get(id);
        if (lastHit == null || System.currentTimeMillis() - lastHit > COMBAT_COOLDOWN) return;

        plugin.getLogger().info("[Logout] " + player.getName() + " logged out in combat!");

        // Create marker armor stand
        ArmorStand marker = player.getWorld().spawn(player.getLocation(), ArmorStand.class, as -> {
            as.setVisible(false);
            as.setMarker(true);
            as.setGravity(false);
            as.setPersistent(true);
            as.setInvulnerable(true);
            as.getPersistentDataContainer().set(MARKER_KEY, PersistentDataType.STRING, id.toString());
            as.getPersistentDataContainer().set(DEAD_KEY, PersistentDataType.INTEGER, 0);

            // Serialize only main inventory (slots 0-35)
            ItemStack[] mainInv = new ItemStack[36];
            System.arraycopy(player.getInventory().getContents(), 0, mainInv, 0, 36);
            as.getPersistentDataContainer().set(INVENTORY_KEY, PersistentDataType.BYTE_ARRAY, ItemSerialization.toBytes(mainInv));

            // Serialize armor separately
            as.getPersistentDataContainer().set(ARMOR_KEY, PersistentDataType.BYTE_ARRAY, ItemSerialization.toBytes(player.getInventory().getArmorContents()));

            // Serialize health
            as.getPersistentDataContainer().set(HEALTH_KEY, PersistentDataType.DOUBLE, player.getHealth());
        });

        // Spawn zombie with marker reference
        Zombie zombie = spawnCombatZombie(player, marker);

        zombieMap.put(id, zombie.getUniqueId());
        zombie.getPersistentDataContainer().set(OWNER_KEY, PersistentDataType.STRING, id.toString());
        zombie.getPersistentDataContainer().set(MARKER_KEY, PersistentDataType.STRING, player.getUniqueId().toString());

        plugin.getLogger().info("[Logout] Spawned zombie for " + player.getName());

        player.getInventory().clear();
        startZombieTimer(id, zombie);
    }

    private void startZombieTimer(UUID playerId, Entity zombie) {
        if (zombie == null || !zombie.isValid()) return;

        BukkitRunnable timer = new BukkitRunnable() {
            @Override
            public void run() {

                zombie.remove();
                zombieMap.remove(playerId);
                zombieTimers.remove(playerId);

                plugin.getLogger().info("[ZombieTimer] Zombie despawned for player " + playerId);
            }
        };
        timer.runTaskLater(plugin, ZOMBIE_LIFETIME / 50L);
        zombieTimers.put(playerId, timer);
    }

    private ArmorStand getMarkerByPlayer(UUID playerId, Chunk chunk) {
        for (Entity e : chunk.getEntities()) {
            if (e instanceof ArmorStand as && as.isMarker()) {
                String owner = as.getPersistentDataContainer().get(MARKER_KEY, PersistentDataType.STRING);
                if (owner != null && owner.equals(playerId.toString())) return as;
            }
        }
        return null;
    }





    // --- Player join ---
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();

        // --- Handle marker first ---
        Chunk spawnChunk = player.getLocation().getChunk();
        ArmorStand marker = getMarkerByPlayer(id, spawnChunk);
        if (marker == null) {
            plugin.getLogger().info("[Login] No marker found for " + player.getName());
            return;
        }

        plugin.getLogger().info("[Login] Marker found: " + marker + " for " + player.getName());

        int deadFlag = marker.getPersistentDataContainer().getOrDefault(DEAD_KEY, PersistentDataType.INTEGER, 0);
        byte[] invBytes = marker.getPersistentDataContainer().get(INVENTORY_KEY, PersistentDataType.BYTE_ARRAY);
        byte[] armorBytes = marker.getPersistentDataContainer().get(ARMOR_KEY, PersistentDataType.BYTE_ARRAY);

        if (deadFlag == 1) {
            plugin.getLogger().info("[Login] Player " + player.getName() + " died while logged out in combat!");
            player.getInventory().clear();
            if (invBytes != null)
                for (ItemStack item : ItemSerialization.fromBytes(invBytes))
                    if (item != null) player.getWorld().dropItemNaturally(player.getLocation(), item);
            if (armorBytes != null)
                for (ItemStack item : ItemSerialization.fromBytes(armorBytes))
                    if (item != null) player.getWorld().dropItemNaturally(player.getLocation(), item);

            player.setHealth(0);
            deathListener.playerActuallyDied(player);
            player.sendMessage("§cYou died while logged out in combat!");
        } else {
            if (invBytes != null) player.getInventory().setContents(ItemSerialization.fromBytes(invBytes));
            if (armorBytes != null) player.getInventory().setArmorContents(ItemSerialization.fromBytes(armorBytes));
            double health = marker.getPersistentDataContainer().getOrDefault(HEALTH_KEY, PersistentDataType.DOUBLE, player.getMaxHealth());
            player.setHealth(Math.min(health, player.getAttribute(Attribute.MAX_HEALTH).getValue()));
            plugin.getLogger().info("[Login] Restored inventory and health(" + health + ") for " + player.getName());
        }

        // --- Remove zombie ---
        UUID zombieId = zombieMap.remove(id);
        Entity zombie = (zombieId != null) ? Bukkit.getEntity(zombieId) : null;
        BukkitRunnable timer = zombieTimers.remove(id);
        if (timer != null) timer.cancel();

        //only scans if zombie cant be found in map (crash/servershutdown or some sort of weird issue)
        if (zombie == null && marker.isValid()) {
            // Only scan the chunk where the marker is
            Chunk chunk = marker.getLocation().getChunk();
            for (Entity e : chunk.getEntities()) {
                if (e instanceof Zombie z) {
                    String ownerStr = z.getPersistentDataContainer().get(OWNER_KEY, PersistentDataType.STRING);
                    if (ownerStr != null && ownerStr.equals(id.toString())) {
                        zombie = z;
                        break;
                    }
                }
            }
        }

        if (zombie != null && zombie.isValid()) {
            zombie.remove();
            plugin.getLogger().info("[Login] Removed leftover zombie for " + player.getName());
        }

        // --- Remove marker ---
        marker.remove();
    }



    // --- Zombie damage updates marker ---
    @EventHandler
    public void onZombieDamage(EntityDamageEvent event) {

        if (!(event.getEntity() instanceof Zombie zombie)) return;

        // Check if zombie is tracked
        String ownerStr = zombie.getPersistentDataContainer().get(OWNER_KEY, PersistentDataType.STRING);
        if (ownerStr == null) return; // Not a combat zombie

        UUID ownerId = UUID.fromString(ownerStr);

        // If zombie is not in map, remove it immediately
        if (!zombieMap.containsKey(ownerId)) {
            plugin.getLogger().info("[ZombieDamage] Untracked zombie detected for player " + ownerId + ", removing it");
            event.setCancelled(true);
            zombie.remove();
            return;
        }

        // Otherwise, update marker
        String playerUUIDStr = zombie.getPersistentDataContainer().get(MARKER_KEY, PersistentDataType.STRING);
        if (playerUUIDStr == null) return;

        ArmorStand marker = getMarkerByPlayer(UUID.fromString(playerUUIDStr), zombie.getChunk());
        if (marker == null) return;

        double currentHealth = zombie.getHealth();
        marker.getPersistentDataContainer().set(HEALTH_KEY, PersistentDataType.DOUBLE, currentHealth);

        zombie.getEquipment();
        marker.getPersistentDataContainer().set(ARMOR_KEY, PersistentDataType.BYTE_ARRAY,
                ItemSerialization.toBytes(zombie.getEquipment().getArmorContents()));

        plugin.getLogger().info("[ZombieDamage] Zombie " + zombie.getCustomName() +
                " took damage, health updated to " + currentHealth + " in marker");
    }


    @EventHandler
    public void onZombieDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;

        PersistentDataContainer pdc = zombie.getPersistentDataContainer();
        if (!pdc.has(OWNER_KEY)) return;

        // Early exit: zombie has no owner => clear drops and return
        if (!pdc.has(OWNER_KEY, PersistentDataType.STRING)) {
            event.getDrops().clear();
            zombie.getEquipment().clear();
            return;
        }

        String raw = pdc.get(OWNER_KEY, PersistentDataType.STRING);
        UUID ownerId;

        // PDC contains invalid UUID => treat as unbound zombie (no drops)
        try {
            ownerId = UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            event.getDrops().clear();
            zombie.getEquipment().clear();
            return;
        }

        ArmorStand marker = getMarkerByPlayer(ownerId, zombie.getChunk());

        // If no marker exists => NO DROPS
        if (marker == null) {
            return;
        }

        // --- VALID MARKER FOUND: handle full logic ---

        event.getDrops().clear();
        zombie.getEquipment().clear();
        event.setDroppedExp(0);
        marker.getPersistentDataContainer().set(DEAD_KEY, PersistentDataType.INTEGER, 1);

        // Drop inventory
        byte[] invBytes = marker.getPersistentDataContainer().get(INVENTORY_KEY, PersistentDataType.BYTE_ARRAY);
        if (invBytes != null) {
            for (ItemStack item : ItemSerialization.fromBytes(invBytes)) {
                if (item != null) zombie.getWorld().dropItemNaturally(zombie.getLocation(), item);
            }
            marker.getPersistentDataContainer().remove(INVENTORY_KEY);
        }

        // Drop armor
        byte[] armorBytes = marker.getPersistentDataContainer().get(ARMOR_KEY, PersistentDataType.BYTE_ARRAY);
        if (armorBytes != null) {
            for (ItemStack item : ItemSerialization.fromBytes(armorBytes)) {
                if (item != null) zombie.getWorld().dropItemNaturally(zombie.getLocation(), item);
            }
            marker.getPersistentDataContainer().remove(ARMOR_KEY);
        }

        zombieMap.remove(ownerId);
        BukkitRunnable timer = zombieTimers.remove(ownerId);
        if (timer != null) timer.cancel();

        zombie.getWorld().playSound(zombie.getLocation(), Sound.ENTITY_ZOMBIE_DEATH, 1f, 1f);
    }


    // --- Spawn combat zombie ---
    private Zombie spawnCombatZombie(Player player, ArmorStand marker) {
        if (marker == null) return null;

        // Deserialize stored inventory and armor
        byte[] invBytes = marker.getPersistentDataContainer().get(INVENTORY_KEY, PersistentDataType.BYTE_ARRAY);
        byte[] armorBytes = marker.getPersistentDataContainer().get(ARMOR_KEY, PersistentDataType.BYTE_ARRAY);
        double storedHealth = marker.getPersistentDataContainer().getOrDefault(HEALTH_KEY, PersistentDataType.DOUBLE, player.getHealth());

        Zombie zombie = (Zombie) player.getWorld().spawnEntity(player.getLocation(), EntityType.ZOMBIE);
        zombie.setCustomName(player.getName());
        zombie.setCustomNameVisible(true);
        zombie.setPersistent(true);
        zombie.setAI(false);
        zombie.setSilent(true);
        zombie.setCanPickupItems(false);
        zombie.setRemoveWhenFarAway(false);
        zombie.setShouldBurnInDay(false);

        zombie.getAttribute(Attribute.MAX_HEALTH).setBaseValue(player.getMaxHealth());
        zombie.setHealth(Math.min(storedHealth, zombie.getAttribute(Attribute.MAX_HEALTH).getValue()));

        // Set main-hand and off-hand from marker inventory
        if (invBytes != null) {
            ItemStack[] inv = ItemSerialization.fromBytes(invBytes);
            zombie.getEquipment().setItemInMainHand(inv.length > 0 ? inv[0] : null);
            zombie.getEquipment().setItemInOffHand(inv.length > 1 ? inv[1] : null);
        }

        // Set armor accurately from stored armor
        if (armorBytes != null) {
            ItemStack[] armor = ItemSerialization.fromBytes(armorBytes);
            zombie.getEquipment().setArmorContents(armor);
        } else {
            zombie.getEquipment().setArmorContents(player.getInventory().getArmorContents());
        }

        plugin.getLogger().info("[SpawnZombie] Spawned zombie for " + player.getName() +
                " | Health: " + zombie.getHealth() + "/" + zombie.getAttribute(Attribute.MAX_HEALTH).getValue());

        return zombie;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) return true;
        combatMap.put(p.getUniqueId(), System.currentTimeMillis());
        plugin.getLogger().info("[Command] /simulatehit executed for " + p.getName());
        p.sendMessage("§0[§0§6CivLabs§0]§8 » §7You are tagged for §ccombat §7for §b" + (COMBAT_COOLDOWN / 1000) + " §7seconds");
        return true;
    }


    // --- Item serialization helper ---
    public static class ItemSerialization {

        public static byte[] toBytes(ItemStack[] items) {
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                 BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
                dataOutput.writeInt(items.length);
                for (ItemStack item : items) dataOutput.writeObject(item);
                return outputStream.toByteArray();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        public static ItemStack[] fromBytes(byte[] bytes) {
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
                 BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
                int length = dataInput.readInt();
                ItemStack[] items = new ItemStack[length];
                for (int i = 0; i < length; i++) items[i] = (ItemStack) dataInput.readObject();
                return items;
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                return new ItemStack[0];
            }
        }
    }
}
