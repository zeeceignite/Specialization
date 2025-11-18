package com.minecraftcivilizations.specialization.Listener.Player;

import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.StaffTools.Debug;
import minecraftcivilizations.com.minecraftCivilizationsCore.MinecraftCivilizationsCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
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
    private static final int DOWNED_DURATION_TICKS = 60 * 20; // 60 seconds
    // --- Add NamespacedKey for remaining ticks ---
    private final NamespacedKey downedTicksKey;
    private final Map<UUID, Integer> downTicksRemaining = new HashMap<>();
    private final Map<UUID, BukkitTask> darknessTasks = new HashMap<>();

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
        Debug.broadcast("down","[DOWNED-DEBUG] setDowned(" + player.getName() + ") = " + downed);

        PersistentDataContainer pdc = player.getPersistentDataContainer();
        pdc.set(downedKey, PersistentDataType.BYTE, (byte) (downed ? 1 : 0));

        if (!downed) {
            Debug.broadcast("down","[DOWNED-DEBUG] setDowned=false → clearDowned called");
            clearDowned(player);
        } else {
            startDowned(player, health, DOWNED_DURATION_TICKS);
            sendDownedMessage(player);
            player.sendMessage(Component.text("You're knocked out").color(NamedTextColor.YELLOW));
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
        Debug.broadcast("down","[DOWNED-DEBUG] clearDowned(" + player.getName() + ")");

        UUID uuid = player.getUniqueId();

        BossBar bar = bossBars.remove(uuid);
        if (bar != null) {
            Debug.broadcast("down","[DOWNED-DEBUG] Removed boss bar");
            bar.removePlayer(player);
        }

        BukkitTask task = downTimers.remove(uuid);
        if (task != null) {
            Debug.broadcast("down","[DOWNED-DEBUG] Cancelled bleedout timer");
            task.cancel();
        }

        Entity e = downStands.remove(uuid);
        if (e != null) {
            Debug.broadcast("down","[DOWNED-DEBUG] Removing downed stand entity");
            e.remove();
        }

        BukkitTask darknessTask = darknessTasks.remove(uuid);
        if (darknessTask != null) {
            darknessTask.cancel();
        }
        player.removePotionEffect(PotionEffectType.DARKNESS);


        player.leaveVehicle();

    }


    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        var pdc = player.getPersistentDataContainer();

        Integer ticksLeft = pdc.get(downedTicksKey, PersistentDataType.INTEGER);


        // --- CASE 1: Player was downed AND ticksLeft exists (normal restore) ---
        if (ticksLeft != null && isDowned(player)) {
            pdc.remove(downedTicksKey);
            clearDowned(player);
            startDowned(player, player.getHealth(), ticksLeft);
            Debug.broadcast("down","[DOWNED-DEBUG] CASE 1 - Ticks:" + ticksLeft + " is downed:" + isDowned(player));
            return;
        }

        // --- CASE 2: Player is downed but ticksLeft is missing (edge case) ---
        if (isDowned(player)) {
            pdc.remove(downedTicksKey);
            clearDowned(player);
            startDowned(player, player.getHealth(), DOWNED_DURATION_TICKS);
            Debug.broadcast("down","[DOWNED-DEBUG] CASE 2 - Is downed:" + player.getName());
            return;
        }

        Debug.broadcast("down","[DOWNED-DEBUG] NO CASE - No downed or ticks for: " + player.getName());
    }



    // --- Damage event to trigger downed ---
    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        Debug.broadcast("down","[DOWNED-DEBUG] DamageEvent: " + player.getName() +
                " dmg=" + event.getFinalDamage() + " hp=" + player.getHealth());

        if (isDowned(player)) {
            Debug.broadcast("down","[DOWNED-DEBUG] " + player.getName() + " is already downed → letting damage occur");
            return; // already downed, let them die
        }

        double finalHealth = player.getHealth() - event.getFinalDamage();
        Debug.broadcast("down","[DOWNED-DEBUG] finalHealth=" + finalHealth);

        if (finalHealth <= 0) {
            Debug.broadcast("down","[DOWNED-DEBUG] Cancelling lethal dmg → triggering downed state.");
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
                if (!isDowned(player)){
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

        // Darkness effect every 5 seconds for 3 seconds
        BukkitTask darknessTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!isDowned(player)) {
                return;
            }
            player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 50, 0, true, false, false));
        }, 200L, 100L); // 200 ticks = 10 seconds
        darknessTasks.put(id, darknessTask);


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


    private void sendDownedMessage(Player player) {
        // [Give Up] button
        Component giveUp = Component.text("[Give Up]", NamedTextColor.RED)
                .clickEvent(ClickEvent.runCommand("/giveup"))
                .hoverEvent(HoverEvent.showText(Component.text("Click to give up and respawn!")));

        Component msg = Component.text("Press Here to ", NamedTextColor.GRAY)
                .append(giveUp)
                .append(Component.text(" & Respawn", NamedTextColor.GRAY));

        // Send main message
        player.sendMessage(msg);

        // If player is allowed to self revive
        if (player.hasPermission("civlabs.selfrevive")) {
            Component revive = Component.text("[Revive]", NamedTextColor.GREEN)
                    .clickEvent(ClickEvent.runCommand("/revive"))
                    .hoverEvent(HoverEvent.showText(Component.text("Click to instantly revive yourself!")));

            player.sendMessage(revive);
        }

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
            p.sendMessage("§7You are knocked out and cannot attack");
        }
    }
}
