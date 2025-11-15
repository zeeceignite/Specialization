package com.minecraftcivilizations.specialization.Listener.Player;

import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import minecraftcivilizations.com.minecraftCivilizationsCore.MinecraftCivilizationsCore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDownedListener implements Listener {

    private final JavaPlugin plugin;
    private final NamespacedKey downedKey;

    private final Map<UUID, BukkitTask> downTimers = new HashMap<>();
    private final Map<UUID, Interaction> downStands = new HashMap<>();
    private final Map<UUID, BossBar> bossBars = new HashMap<>();
    private static final int DOWNED_DURATION_TICKS = 20 * 20; // 20 seconds
    // --- Add NamespacedKey for remaining ticks ---
    private final NamespacedKey downedTicksKey;
    private final Map<UUID, Integer> downTicksRemaining = new HashMap<>();


    public PlayerDownedListener(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        this.downedKey = new NamespacedKey(plugin, "is_downed");

        this.downedTicksKey = new NamespacedKey(plugin, "downed_ticks");
    }

    // --- PDC getters/setters ---
    public boolean isDowned(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        return pdc.has(downedKey, PersistentDataType.BYTE) && pdc.get(downedKey, PersistentDataType.BYTE) == 1;
    }

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

    // On join
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Integer ticksLeft = player.getPersistentDataContainer().get(downedTicksKey, PersistentDataType.INTEGER);
        if (ticksLeft != null) {
            player.getPersistentDataContainer().remove(downedTicksKey);
            startDowned(player, player.getHealth(), ticksLeft);
        }
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
            event.setCancelled(true);
            setDowned(player, true, 10);
        }
    }

    // On quit
    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!isDowned(player)) return;

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
        player.setHealth(health);
        Location loc = player.getLocation().clone().subtract(0, 0.5, 0);

        // Spawn interaction entity (correct one)
        Interaction inter = player.getWorld().spawn(loc, Interaction.class, i -> {
            i.setInteractionWidth(0.6f);
            i.setInteractionHeight(0.6f);
            i.setResponsive(false);
            i.setInvulnerable(true);
            i.setGravity(false);
            i.setSilent(true);
        });

        player.leaveVehicle();
        inter.addPassenger(player);
        downStands.put(id, inter);


        // boss bar
        BossBar bar = Bukkit.createBossBar("§8Bleeding out", BarColor.RED, BarStyle.SOLID);
        bar.addPlayer(player);
        bossBars.put(id, bar);

        // start timer


        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int ticksLeft = remainingTicks;

            @Override
            public void run() {
                if (!player.isOnline() || !isDowned(player)) {
                    clearDowned(player);
                    return;
                }

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

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (!isDowned(player)) return;

        // Cancel toggle
        event.setCancelled(true);

        // Force client to unsneak
        player.setSneaking(false);

        // Re-mount if somehow dismounted
        Interaction inter = downStands.get(player.getUniqueId());
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


    // --- Prevent interactions while downed ---
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player p && isDowned(p)) {
            Bukkit.getLogger().info("[DOWNED-DEBUG] Downed player tried to attack: " + p.getName());
            event.setCancelled(true);
            p.sendMessage("§cYou are downed and cannot attack!");
        }
    }
}
