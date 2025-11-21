package com.minecraftcivilizations.specialization.Listener.Player;

import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillLevel;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.Specialization;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LeashHitch;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class LeashListener implements Listener {

    private final List<Player> leashedPlayers = new ArrayList<>();
    private final List<LivingEntity> leashEntities = new ArrayList<>();
    private final HashMap<Player, BukkitRunnable> activeRunnables = new HashMap<>();
    // Map to track which zombie belongs to which player to prevent duplicates
    private final HashMap<Player, LivingEntity> playerToZombie = new HashMap<>();
    // Map to track players leashed to fence posts
    private final HashMap<Player, LeashHitch> playerToFence = new HashMap<>();
    PlayerDownedListener playerDownedListener = new PlayerDownedListener(Specialization.getInstance());

    @EventHandler
    public void onUnleash(EntityUnleashEvent e) {
        if (e.getReason() == EntityUnleashEvent.UnleashReason.PLAYER_UNLEASH) return;
        if (leashEntities.contains(e.getEntity())) {
            // Find and unleash the player associated with this zombie
            Player targetPlayer = findPlayerByZombie((LivingEntity) e.getEntity());
            if (targetPlayer != null) {
                unleashPlayer(targetPlayer);
            }
        }
    }

    @EventHandler
    public void onLeash(PlayerInteractAtEntityEvent e) {
        if (!(e.getRightClicked() instanceof Player)) return;
        if (!e.getHand().equals(EquipmentSlot.HAND)) return;

        Player player = e.getPlayer();
        Player target = (Player) e.getRightClicked();

        // Check if player has lead
        if (!player.getInventory().getItemInMainHand().getType().equals(Material.LEAD)) return;
        // Check if player is grandmaster guardsman
        CustomPlayer leashHolder = CoreUtil.getPlayer(player);
        if (leashHolder.getSkillLevel(SkillType.GUARDSMAN) < SkillLevel.GRANDMASTER.getLevel()) {
            player.sendMessage("Only grandmaster guardsmen can leash players.");
            return;
        }

        // If target is already leashed, unleash them
        if (leashedPlayers.contains(target)) {
            unleashPlayer(target);
            return;
        }

        // Check if target is downed
        if (!playerDownedListener.isDowned(player)) {
            player.sendMessage("You can only leash downed players.");
            return;
        }
        playerDownedListener.clearMount(player);


        // Prevent duplicate zombies - check if player already has a zombie
        if (playerToZombie.containsKey(target)) {
            // Clean up existing zombie first
            LivingEntity existingZombie = playerToZombie.get(target);
            if (existingZombie != null && existingZombie.isValid()) {
                existingZombie.remove();
            }
            playerToZombie.remove(target);
            leashEntities.remove(existingZombie);
        }

        // Create invisible zombie for leashing
        LivingEntity zombie = target.getWorld().spawn(target.getLocation(), Zombie.class, zombo -> {
            // Clear equipment
            zombo.getEquipment().setItemInMainHand(null);
            zombo.getEquipment().setHelmet(null);
            zombo.getEquipment().setChestplate(null);
            zombo.getEquipment().setLeggings(null);
            zombo.getEquipment().setBoots(null);
            zombo.setCanPickupItems(false);

            // Configure zombie properties
            zombo.setSilent(true);
            zombo.setInvisible(true);
            zombo.setCollidable(false);
            zombo.setInvulnerable(true);
            zombo.setAI(true); // Enable zombie AI
            zombo.setGravity(true);

            // Add potion effects
            zombo.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, Integer.MAX_VALUE, 255, false, false));
            zombo.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 255, false, false));

            // Set leash holder
            zombo.setLeashHolder(player);
        });

        // Add to tracking lists and maps
        leashedPlayers.add(target);
        leashEntities.add(zombie);
        playerToZombie.put(target, zombie); // Track zombie-player relationship

        // Update custom player data
        CoreUtil.getPlayer(target).setLeashedTo(player.getUniqueId());

        // Safely add to leashed other players list
        CustomPlayer customPlayer = CoreUtil.getPlayer(player);
        if (customPlayer.getLeashedOtherPlayers() != null) {
            customPlayer.getLeashedOtherPlayers().add(target.getUniqueId());
        }

        // Consume lead
        player.getInventory().getItemInMainHand().setAmount(player.getInventory().getItemInMainHand().getAmount() - 1);

        // Ensure flight is disabled for natural gravity
        target.setAllowFlight(false);
        target.setFlying(false);

        BukkitRunnable syncTask = new BukkitRunnable() {
            private Location lastZombieLocation = zombie.getLocation().clone();

            @Override
            public void run() {
                // Check if leash should be removed
                if (!target.isOnline() || !zombie.isValid() || !zombie.isLeashed() || !leashedPlayers.contains(target)) {
                    unleashPlayer(target);
                    cancel();
                    return;
                }

                // Check if zombie is leashed to a fence post
                if (zombie.getLeashHolder() instanceof LeashHitch) {
                    // Player is now leashed to a fence, handle differently
                    handleFenceLeash(target, zombie, (LeashHitch) zombie.getLeashHolder());
                    cancel();
                    return;
                }

                // Get current locations
                Location zombieLoc = zombie.getLocation();
                Location playerLoc = target.getLocation();

                // Only teleport if zombie has moved significantly OR if player is too far from zombie
                double zombieMovement = lastZombieLocation.distanceSquared(zombieLoc);
                double playerZombieDistance = playerLoc.distanceSquared(zombieLoc);

                if (zombieMovement > 0.0025 || playerZombieDistance > 0.001) { // Much smaller thresholds for smoother movement
                    // Create new location using zombie's position but DON'T specify rotation
                    // This preserves the player's current clientside head rotation
                    Location newPlayerLocation = new Location(
                            target.getWorld(),
                            zombieLoc.getX(),
                            zombieLoc.getY(),
                            zombieLoc.getZ()
                            // IMPORTANT: Don't set yaw/pitch - let it default to preserve head movement
                    );

                    target.teleport(newPlayerLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
                    lastZombieLocation = zombieLoc.clone();
                }
            }
        };

        syncTask.runTaskTimer(Specialization.getInstance(), 0, 1);// Run every 2 ticks instead of every tick // Run every tick for smooth movement
        activeRunnables.put(target, syncTask);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        CustomPlayer player = CoreUtil.getPlayer(e);
        if (player.getLeashedTo() != null) {
            Player leashHolder = Bukkit.getPlayer(player.getLeashedTo());
            if (leashHolder != null && leashHolder.isOnline()) {
                // Check if leash holder is still grandmaster guardsman
                CustomPlayer leashHolderCustomPlayer = CoreUtil.getPlayer(leashHolder);
                if (leashHolderCustomPlayer.getSkillLevel(SkillType.GUARDSMAN) >= SkillLevel.GRANDMASTER.getLevel()) {
                    // Recreate leash on login - target was leashed so they shouldn't be downed
                    recreateLeash(leashHolder, e.getPlayer());
                } else {
                    // Leash holder no longer qualifies, clear leash state
                    player.setLeashedTo(null);
                }
            } else {
                player.setLeashedTo(null);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        // Clean up zombie and lead on logout but preserve leashed state
        if (leashedPlayers.contains(e.getPlayer())) {
            // Find and remove the zombie entity (but keep leashed state in CustomPlayer)
            LivingEntity zombieToRemove = playerToZombie.remove(e.getPlayer());
            if (zombieToRemove != null) {
                leashEntities.remove(zombieToRemove);
                // Remove the zombie entity (lead will be deleted with it)
                zombieToRemove.remove();
            }

            // Clean up fence leash tracking
            playerToFence.remove(e.getPlayer());

            // Remove from active tracking but keep CustomPlayer leashed state
            leashedPlayers.remove(e.getPlayer());

            // Cancel sync task
            BukkitRunnable runnable = activeRunnables.remove(e.getPlayer());
            if (runnable != null) {
                runnable.cancel();
            }
        }

        // Clean up if this player was leashing others
        CustomPlayer player = CoreUtil.getPlayer(e);
        if (player != null && player.getLeashedOtherPlayers() != null && !player.getLeashedOtherPlayers().isEmpty()) {
            List<UUID> toRemove = new ArrayList<>(player.getLeashedOtherPlayers());
            for (UUID uuid : toRemove) {
                Player leashedPlayer = Bukkit.getPlayer(uuid);
                if (leashedPlayer != null) {
                    unleashPlayer(leashedPlayer);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        // Clean up leash on death
        if (leashedPlayers.contains(e.getPlayer())) {
            unleashPlayer(e.getPlayer());
        }
    }

    @EventHandler
    public void onUnLeash(PlayerUnleashEntityEvent e) {
        if (e.getEntity() instanceof LivingEntity living) {
            if (leashEntities.contains(living)) {
                leashEntities.remove(living);
                living.remove();
            }
        }

    }

    @EventHandler
    public void onEntityCombust(EntityCombustEvent e) {
        if (leashEntities.contains(e.getEntity())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        if (leashEntities.contains(e.getDamager()) || leashEntities.contains(e.getEntity())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onTryLeashToBlock(PlayerLeashEntityEvent e) {
        // Allow leashing to fence posts for leashed players
        if (e.getLeashHolder() instanceof LeashHitch && leashEntities.contains(e.getEntity())) {
            // Don't cancel - allow the leash to attach to fence
            return;
        }
    }

    private void handleFenceLeash(Player target, LivingEntity zombie, LeashHitch leashHitch) {
        // Store fence leash information
        playerToFence.put(target, leashHitch);

        // Create a new sync task for fence-leashed players
        BukkitRunnable fenceTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Check if leash should be removed
                if (!target.isOnline() || !zombie.isValid() || !zombie.isLeashed() || !leashedPlayers.contains(target)) {
                    unleashPlayer(target);
                    cancel();
                    return;
                }

                // Check if still leashed to fence
                if (!(zombie.getLeashHolder() instanceof LeashHitch)) {
                    // No longer leashed to fence, player can move freely within leash range
                    playerToFence.remove(target);
                    // Resume normal leash behavior
                    startNormalLeashTask(target, zombie);
                    cancel();
                    return;
                }

                // Keep player within leash range of the fence
                Location fenceLocation = leashHitch.getLocation();
                Location playerLocation = target.getLocation();

                double distance = fenceLocation.distance(playerLocation);
                double maxDistance = 10.0; // Maximum leash distance

                if (distance > maxDistance) {
                    // Pull player back towards fence
                    Location newLocation = fenceLocation.clone();
                    newLocation.setDirection(playerLocation.clone().subtract(fenceLocation).toVector().normalize().multiply(maxDistance));
                    newLocation.add(newLocation.getDirection());
                    newLocation.setY(fenceLocation.getY()); // Keep at fence level

                    target.teleport(newLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
                }
            }
        };

        fenceTask.runTaskTimer(Specialization.getInstance(), 0, 2); // Run every 2 ticks for fence leash
        activeRunnables.put(target, fenceTask);
    }

    private void startNormalLeashTask(Player target, LivingEntity zombie) {
        BukkitRunnable syncTask = new BukkitRunnable() {
            private Location lastZombieLocation = zombie.getLocation().clone();

            @Override
            public void run() {
                // Check if leash should be removed
                if (!target.isOnline() || !zombie.isValid() || !zombie.isLeashed() || !leashedPlayers.contains(target)) {
                    unleashPlayer(target);
                    cancel();
                    return;
                }

                // Check if zombie is leashed to a fence post again
                if (zombie.getLeashHolder() instanceof LeashHitch) {
                    // Player is now leashed to a fence, handle differently
                    handleFenceLeash(target, zombie, (LeashHitch) zombie.getLeashHolder());
                    cancel();
                    return;
                }

                // Get current locations
                Location zombieLoc = zombie.getLocation();
                Location playerLoc = target.getLocation();

                // Only teleport if zombie has moved significantly OR if player is too far from zombie
                double zombieMovement = lastZombieLocation.distanceSquared(zombieLoc);
                double playerZombieDistance = playerLoc.distanceSquared(zombieLoc);

                if (zombieMovement > 0.0025 || playerZombieDistance > 0.001) {
                    Location newPlayerLocation = new Location(
                            target.getWorld(),
                            zombieLoc.getX(),
                            zombieLoc.getY(),
                            zombieLoc.getZ()
                    );

                    target.teleport(newPlayerLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
                    lastZombieLocation = zombieLoc.clone();
                }
            }
        };

        syncTask.runTaskTimer(Specialization.getInstance(), 0, 1);
        activeRunnables.put(target, syncTask);
    }

    private void unleashPlayer(Player target) {
        if (!leashedPlayers.contains(target)) return;

        // Remove from tracking lists
        leashedPlayers.remove(target);

        // Find and remove the zombie using the player-zombie mapping
        LivingEntity zombieToRemove = playerToZombie.remove(target);
        if (zombieToRemove != null) {
            leashEntities.remove(zombieToRemove);

            // Remove the zombie entity
            zombieToRemove.remove();
        }

        // Clean up fence leash tracking
        playerToFence.remove(target);

        // Also clean up any orphaned zombies for this player (fallback cleanup)
        cleanupOrphanedZombies(target);

        // Cancel sync task
        BukkitRunnable runnable = activeRunnables.remove(target);
        if (runnable != null) {
            runnable.cancel();
        }

        // Ensure flight is disabled
        target.setAllowFlight(false);
        target.setFlying(false);

        // Update custom player data
        CustomPlayer customTarget = CoreUtil.getPlayer(target);
        if (customTarget.getLeashedTo() != null) {
            Player leashHolder = Bukkit.getPlayer(customTarget.getLeashedTo());
            if (leashHolder != null) {
                CustomPlayer customLeashHolder = CoreUtil.getPlayer(leashHolder);
                if (customLeashHolder.getLeashedOtherPlayers() != null) {
                    customLeashHolder.getLeashedOtherPlayers().remove(target.getUniqueId());
                }
            }
            customTarget.setLeashedTo(null);
        }

        // Put target back into downed state
        playerDownedListener.clearMount(target);
        playerDownedListener.setSit(target);
    }

    // Helper method to find which player is associated with a zombie
    private Player findPlayerByZombie(LivingEntity zombie) {
        for (HashMap.Entry<Player, LivingEntity> entry : playerToZombie.entrySet()) {
            if (entry.getValue().equals(zombie)) {
                return entry.getKey();
            }
        }
        return null;
    }

    // Cleanup method to remove any orphaned zombies for a player
    private void cleanupOrphanedZombies(Player target) {
        List<LivingEntity> toRemove = new ArrayList<>();
        for (LivingEntity entity : leashEntities) {
            if (entity instanceof Zombie && entity.getLeashHolder() != null) {
                // Check if this zombie was meant for this target player
                CustomPlayer customTarget = CoreUtil.getPlayer(target);
                if (customTarget.getLeashedTo() != null &&
                        entity.getLeashHolder().getUniqueId().equals(customTarget.getLeashedTo())) {
                    toRemove.add(entity);
                }
            }
        }

        // Remove orphaned zombies
        for (LivingEntity zombie : toRemove) {
            leashEntities.remove(zombie);
            zombie.remove();
        }
    }

    private void recreateLeash(Player leashHolder, Player target) {
        // Prevent duplicate zombies - check if player already has a zombie
        if (playerToZombie.containsKey(target)) {
            LivingEntity existingZombie = playerToZombie.get(target);
            if (existingZombie != null && existingZombie.isValid()) {
                existingZombie.remove();
            }
            playerToZombie.remove(target);
            leashEntities.remove(existingZombie);
        }

        // This method recreates a leash when a player logs back in
        // Similar to addTracker but without consuming items or permission checks
        LivingEntity zombie = target.getWorld().spawn(target.getLocation(), Zombie.class, zombo -> {
            zombo.getEquipment().setItemInMainHand(null);
            zombo.getEquipment().setHelmet(null);
            zombo.getEquipment().setChestplate(null);
            zombo.getEquipment().setLeggings(null);
            zombo.getEquipment().setBoots(null);
            zombo.setCanPickupItems(false);
            zombo.setSilent(true);
            zombo.setInvisible(true);
            zombo.setCollidable(false);
            zombo.setInvulnerable(true);
            zombo.setAI(true); // Enable zombie AI
            zombo.setGravity(true);
            zombo.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, Integer.MAX_VALUE, 255, false, false));
            zombo.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 255, false, false));
            zombo.setLeashHolder(leashHolder);
        });

        // Set the leash holder AFTER the zombie is fully spawned and initialized
        // Use a delayed task to ensure the zombie is properly loaded
        new BukkitRunnable() {
            @Override
            public void run() {
                if (zombie.isValid() && leashHolder.isOnline()) {
                    zombie.setLeashHolder(leashHolder);
                }
            }
        }.runTaskLater(Specialization.getInstance(), 1L); // Wait 1 tick

        leashedPlayers.add(target);
        leashEntities.add(zombie);
        playerToZombie.put(target, zombie); // Track zombie-player relationship

        // Ensure flight is disabled
        target.setAllowFlight(false);
        target.setFlying(false);

        // Update custom player data
        CustomPlayer customTarget = CoreUtil.getPlayer(target);
        customTarget.setDowned(false); // Transition out of downed state

        BukkitRunnable syncTask = new BukkitRunnable() {
            private Location lastZombieLocation = zombie.getLocation().clone();

            @Override
            public void run() {
                if (!target.isOnline() || !zombie.isValid() || !zombie.isLeashed() || !leashedPlayers.contains(target)) {
                    unleashPlayer(target);
                    cancel();
                    return;
                }

                // Check if zombie is leashed to a fence post
                if (zombie.getLeashHolder() instanceof LeashHitch) {
                    // Player is now leashed to a fence, handle differently
                    handleFenceLeash(target, zombie, (LeashHitch) zombie.getLeashHolder());
                    cancel();
                    return;
                }

                // Get current locations
                Location zombieLoc = zombie.getLocation();
                Location playerLoc = target.getLocation();

                // Only teleport if zombie has moved significantly OR if player is too far from zombie
                double zombieMovement = lastZombieLocation.distanceSquared(zombieLoc);
                double playerZombieDistance = playerLoc.distanceSquared(zombieLoc);

                if (zombieMovement > 0.0025 || playerZombieDistance > 0.001) { // Much smaller thresholds for smoother movement
                    // Create new location using zombie's position but DON'T specify rotation
                    // This preserves the player's current clientside head rotation
                    Location newPlayerLocation = new Location(
                            target.getWorld(),
                            zombieLoc.getX(),
                            zombieLoc.getY(),
                            zombieLoc.getZ()
                            // IMPORTANT: Don't set yaw/pitch - let it default to preserve head movement
                    );

                    target.teleport(newPlayerLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
                    lastZombieLocation = zombieLoc.clone();
                }
            }
        };

        syncTask.runTaskTimer(Specialization.getInstance(), 2, 1);
        activeRunnables.put(target, syncTask);
    }
}