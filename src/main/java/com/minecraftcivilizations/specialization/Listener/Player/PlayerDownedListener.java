package com.minecraftcivilizations.specialization.Listener.Player;

import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import minecraftcivilizations.com.minecraftCivilizationsCore.MinecraftCivilizationsCore;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDownedListener implements Listener {

    private final JavaPlugin plugin;
    private final NamespacedKey downedKey;

    private final Map<UUID, BukkitTask> downTimers = new HashMap<>();
    private final Map<UUID, Entity> downStands = new HashMap<>();
    private final Map<UUID, BossBar> bossBars = new HashMap<>();
    private static final int DOWNED_DURATION_TICKS = 20 * 20; // 20 seconds
    // --- Add NamespacedKey for remaining ticks ---
    private final NamespacedKey downedTicksKey;
    private final Map<UUID, Integer> downTicksRemaining = new HashMap<>();
    private static int joinEventCounter = 0; // counter for join events

    public PlayerDownedListener(JavaPlugin plugin) {
        this.plugin = plugin;
        this.downedKey = new NamespacedKey(plugin, "is_downed");
        this.downedTicksKey = new NamespacedKey(plugin, "downed_ticks");
    }


    // --- PDC getters/setters ---
    public boolean isDowned(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        return pdc.has(downedKey, PersistentDataType.BYTE) && pdc.get(downedKey, PersistentDataType.BYTE) == 1;
    }


    //Call this to handle state changes. It can handle everything else.
    public void setDowned(Player player, boolean downed, double health) {
        Bukkit.getLogger().info("[DOWNED-DEBUG] setDowned(" + player.getName() + ") = " + downed);

        PersistentDataContainer pdc = player.getPersistentDataContainer();
        pdc.set(downedKey, PersistentDataType.BYTE, (byte) (downed ? 1 : 0));

        if (!downed) {
            Bukkit.getLogger().info("[DOWNED-DEBUG] setDowned=false → clearDowned called");
            clearDowned(player);
        } else {
            startDowned(player, health, DOWNED_DURATION_TICKS);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        CustomPlayer customPlayer = (CustomPlayer) MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().getCustomPlayer(player.getUniqueId());
        // Reset all skills to 0
        for (SkillType type : SkillType.values()) {
            double currentXp = customPlayer.getSkill(type).getXp();
            customPlayer.addSkillXp(type, -currentXp, null, true, false); // subtract current XP to zero it
        }

        if (isDowned(player)) {
            setDowned(player, false, 0);
        }
    }


    // --- Clear downed state ---
    private void clearDowned(Player player) {
        Bukkit.getLogger().info("[DOWNED-DEBUG] clearDowned(" + player.getName() + ")");

        UUID uuid = player.getUniqueId();

        BossBar bar = bossBars.remove(uuid);
        if (bar != null) {
            Bukkit.getLogger().info("[DOWNED-DEBUG] Removed boss bar");
            bar.removePlayer(player);
        }

        BukkitTask task = downTimers.remove(uuid);
        if (task != null) {
            Bukkit.getLogger().info("[DOWNED-DEBUG] Cancelled bleedout timer");
            task.cancel();
        }

        Entity e = downStands.remove(uuid);
        if (e != null) {
            Bukkit.getLogger().info("[DOWNED-DEBUG] Removing downed stand entity");
            e.remove();
        }

        player.leaveVehicle();

    }


    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        joinEventCounter++; // increment each time event is called
        Player player = event.getPlayer();
        var pdc = player.getPersistentDataContainer();

        Integer ticksLeft = pdc.get(downedTicksKey, PersistentDataType.INTEGER);

        Bukkit.getLogger().info("[DOWNED-DEBUG] onPlayerJoin called " + joinEventCounter + " times for player " + player.getName());

        // --- CASE 1: Player was downed AND ticksLeft exists (normal restore) ---
        if (ticksLeft != null && isDowned(player)) {
            pdc.remove(downedTicksKey);
            clearDowned(player);
            startDowned(player, player.getHealth(), ticksLeft);
            Bukkit.getLogger().info("[DOWNED-DEBUG] CASE 1 - Ticks:" + ticksLeft + " is downed:" + isDowned(player));
            return;
        }

        // --- CASE 2: Player is downed but ticksLeft is missing (edge case) ---
        if (isDowned(player)) {
            pdc.remove(downedTicksKey);
            clearDowned(player);
            startDowned(player, player.getHealth(), DOWNED_DURATION_TICKS);
            Bukkit.getLogger().info("[DOWNED-DEBUG] CASE 2 - Is downed:" + player.getName());
            return;
        }

        Bukkit.getLogger().info("[DOWNED-DEBUG] NO CASE - No downed or ticks for: " + player.getName());
    }



    // --- Damage event to trigger downed ---
    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        Bukkit.getLogger().info("[DOWNED-DEBUG] DamageEvent: " + player.getName() +
                " dmg=" + event.getFinalDamage() + " hp=" + player.getHealth());

        if (isDowned(player)) {
            Bukkit.getLogger().info("[DOWNED-DEBUG] " + player.getName() + " is already downed → letting damage occur");
            return; // already downed, let them die
        }

        double finalHealth = player.getHealth() - event.getFinalDamage();
        Bukkit.getLogger().info("[DOWNED-DEBUG] finalHealth=" + finalHealth);

        if (finalHealth <= 0) {
            Bukkit.getLogger().info("[DOWNED-DEBUG] Cancelling lethal dmg → triggering downed state.");
            if (finalHealth <= -10) {
                return;
            }
            event.setCancelled(true);
            setDowned(player, true, 10 + finalHealth);
        }
    }

    // On quit
    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!isDowned(player)) return;
        clearDowned(player);
        UUID id = player.getUniqueId();
        Integer ticksLeft = downTicksRemaining.get(id);
        if (ticksLeft != null) {
            player.getPersistentDataContainer().set(downedTicksKey, PersistentDataType.INTEGER, ticksLeft);
        }

        BukkitTask task = downTimers.remove(id);
        if (task != null) task.cancel();
    }



    // Adjusted startDowned
    private void startDowned(Player player, double health, int remainingTicks) {
        UUID id = player.getUniqueId();
        downTicksRemaining.put(id, remainingTicks);
        player.setHealth(Math.max(0, health));

        Location loc = player.getLocation().clone();
        Block blockBelow = findBlockBelow(loc);
        double distance = blockBelow.getY() + 1.0 - loc.getY();

        if (distance * -1 > 1.0) {
            // Use ArmorStand for falling
            player.sendMessage("armorstand");
            ArmorStand stand = player.getWorld().spawn(loc, ArmorStand.class, a -> {
                a.setGravity(true);
                a.setInvulnerable(true);
                a.setVisible(false);
                a.setCollidable(false);
                a.setMarker(false);
                a.setArms(false);
                a.addPassenger(player);
                a.getAttribute(Attribute.SCALE).setBaseValue(0.01);

            });
            downStands.put(id, stand); // store as Entity
        } else {
            // Use Interaction for precise sitting
            Location locInteraction = player.getLocation().clone().subtract(0, 0.5, 0);
            player.sendMessage("interaction" + distance);
            Interaction inter = player.getWorld().spawn(locInteraction, Interaction.class, i -> {
                i.setInteractionWidth(0.6f);
                i.setInteractionHeight(0.6f);
                i.setInvulnerable(true);
                i.setSilent(true);
                i.setPersistent(false);
                i.addPassenger(player);
            });
            downStands.put(id, inter);
        }

        // BossBar & bleedout timer
        BossBar bar = Bukkit.createBossBar("§8Bleeding out", BarColor.RED, BarStyle.SOLID);
        bar.addPlayer(player);
        bossBars.put(id, bar);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int ticksLeft = remainingTicks;

            @Override
            public void run() {
                if (!player.isOnline() || !isDowned(player)) return;

                ticksLeft--;
                downTicksRemaining.put(id, ticksLeft);
                bar.setProgress(Math.max(0f, ticksLeft / (float) DOWNED_DURATION_TICKS));

                if (ticksLeft <= 0) {
                    player.setHealth(0);
                    clearDowned(player);
                    player.damage(100);
                }
            }
        }, 1L, 1L);

        downTimers.put(id, task);
    }



    // --- Prevent interactions while downed ---
    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (!isDowned(player)) return;

        // Cancel toggle
        event.setCancelled(true);

        // Force client to unsneak
        player.setSneaking(false);

        // Re-mount if somehow dismounted
        Entity inter = downStands.get(player.getUniqueId());
        if (inter != null && inter.isValid()) {
            if (!inter.getPassengers().contains(player)) {
                inter.addPassenger(player);
            }
        }
    }


    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!isDowned(player)) return;

        // Only cancel positional movement
        if (event.getFrom().getX() != event.getTo().getX() ||
                event.getFrom().getY() != event.getTo().getY() ||
                event.getFrom().getZ() != event.getTo().getZ()) {
            Location from = event.getFrom();
            Location to = event.getTo();

            // Cancel movement but keep rotation
            to.setX(from.getX());
            to.setY(from.getY());
            to.setZ(from.getZ());
            event.setTo(to);
        }
    }

    private boolean cancelIfDowned(Player p, Cancellable event) {
        if (!isDowned(p)) return false;
        event.setCancelled(true);
        return true;
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            Player p = event.getPlayer();
            if (isDowned(p)) {
                event.setCancelled(true);
            }
        }
    }




    private Block findBlockBelow(Location loc) {
        World world = loc.getWorld();
        int y = loc.getBlockY();

        for (int i = y; i > 0; i--) {
            Block block = world.getBlockAt(loc.getBlockX(), i, loc.getBlockZ());
            if (!block.isPassable()) {
                if (block.getType() == Material.WATER) {
                    return null; // stop falling into water
                }
                return block;
            }
        }

        return world.getBlockAt(loc.getBlockX(), 0, loc.getBlockZ()); // fallback floor
    }



    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player p)
            cancelIfDowned(p, event);}

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {cancelIfDowned(event.getPlayer(), event);}

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {cancelIfDowned(event.getPlayer(), event);}

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {cancelIfDowned(event.getPlayer(), event);}

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player p && cancelIfDowned(p, event)) {
            p.sendMessage("§cYou are downed and cannot attack!");
        }
    }
}
