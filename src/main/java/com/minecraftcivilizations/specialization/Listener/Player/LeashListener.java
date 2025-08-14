package com.minecraftcivilizations.specialization.Listener.Player;

import com.minecraftcivilizations.specialization.Player.CustomPlayer;
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
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityUnleashEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
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
    private final List<LivingEntity> distanceUnleash = new ArrayList<>();
    private final HashMap<Player, BukkitRunnable> activeRunnables = new HashMap<>();

    @EventHandler
    public void onUnleash(EntityUnleashEvent e) {
        if(e.getReason() == EntityUnleashEvent.UnleashReason.PLAYER_UNLEASH) return;
        if(leashEntities.contains(e.getEntity())) {
            distanceUnleash.add((LivingEntity) e.getEntity());
        }
    }

    @EventHandler
    public void onLeash(PlayerInteractAtEntityEvent e) {
        if(!(e.getRightClicked() instanceof Player)) return;
        if(!e.getHand().equals(EquipmentSlot.HAND)) return;

        Player player = e.getPlayer();
        Player target = (Player) e.getRightClicked();

        // Check if player has lead
        if(!player.getInventory().getItemInMainHand().getType().equals(Material.LEAD)) return;

        // If target is already leashed, unleash them
        if(leashedPlayers.contains(target)) {
            unleashPlayer(target);
            return;
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
            
            // Add potion effects
            zombo.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, Integer.MAX_VALUE, 255, false, false));
            zombo.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 255, false, false));
            
            // Set leash holder
            zombo.setLeashHolder(player);
        });

        // Add to tracking lists
        leashedPlayers.add(target);
        leashEntities.add(zombie);

        // Update custom player data
        CoreUtil.getPlayer(target).setLeashedTo(player.getUniqueId());
        
        // Safely add to leashed other players list
        CustomPlayer customPlayer = CoreUtil.getPlayer(player);
        if(customPlayer.getLeashedOtherPlayers() != null) {
            customPlayer.getLeashedOtherPlayers().add(target.getUniqueId());
        }

        // Consume lead
        player.getInventory().getItemInMainHand().setAmount(player.getInventory().getItemInMainHand().getAmount() - 1);

        // Allow flight for smoother movement
        target.setAllowFlight(true);

        // Start continuous sync task
        BukkitRunnable syncTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Check if leash should be removed
                if(!target.isOnline() || !zombie.isValid() || !zombie.isLeashed() || !leashedPlayers.contains(target)) {
                    unleashPlayer(target);
                    cancel();
                    return;
                }

                // Teleport player to zombie location while preserving pitch/yaw
                Location targetLoc = target.getLocation();
                Location zombieLoc = zombie.getLocation();
                
                targetLoc.setX(zombieLoc.getX());
                targetLoc.setY(zombieLoc.getY());
                targetLoc.setZ(zombieLoc.getZ());
                
                target.teleport(targetLoc, PlayerTeleportEvent.TeleportCause.PLUGIN);
            }
        };
        
        syncTask.runTaskTimer(Specialization.getInstance(), 0, 1); // Run every tick for smooth movement
        activeRunnables.put(target, syncTask);
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent e) {
        CustomPlayer player = CoreUtil.getPlayer(e);
        if(player.getLeashedTo() != null){
            Player leashHolder = Bukkit.getPlayer(player.getLeashedTo());
            if(leashHolder != null && leashHolder.isOnline()){
                // Recreate leash on login
                Bukkit.getScheduler().runTaskLater(Specialization.getInstance(), () -> {
                    recreateLeash(leashHolder, e.getPlayer());
                }, 5L); // Delay to ensure player is fully loaded
            } else {
                player.setLeashedTo(null);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        // Clean up if this player was leashed
        if(leashedPlayers.contains(e.getPlayer())) {
            unleashPlayer(e.getPlayer());
        }
        
        // Clean up if this player was leashing others
        CustomPlayer player = CoreUtil.getPlayer(e);
        if(player.getLeashedOtherPlayers() != null && !player.getLeashedOtherPlayers().isEmpty()) {
            List<UUID> toRemove = new ArrayList<>(player.getLeashedOtherPlayers());
            for(UUID uuid : toRemove) {
                Player leashedPlayer = Bukkit.getPlayer(uuid);
                if(leashedPlayer != null) {
                    unleashPlayer(leashedPlayer);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        // Clean up leash on death
        if(leashedPlayers.contains(e.getPlayer())) {
            unleashPlayer(e.getPlayer());
        }
    }

    @EventHandler
    public void onEntityCombust(EntityCombustEvent e) {
        if(leashEntities.contains(e.getEntity())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        if(leashEntities.contains(e.getDamager()) || leashEntities.contains(e.getEntity())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onTryLeashToBlock(PlayerLeashEntityEvent e){
        if(e.getLeashHolder() instanceof LeashHitch && leashEntities.contains(e.getEntity())){
            e.setCancelled(true);
        }
    }

    private void unleashPlayer(Player target) {
        if(!leashedPlayers.contains(target)) return;
        
        // Remove from tracking lists
        leashedPlayers.remove(target);
        
        // Find and remove the zombie
        LivingEntity zombieToRemove = null;
        for(LivingEntity entity : leashEntities) {
            if(entity.getLeashHolder() != null && 
               CoreUtil.getPlayer(target).getLeashedTo() != null &&
               entity.getLeashHolder().getUniqueId().equals(CoreUtil.getPlayer(target).getLeashedTo())) {
                zombieToRemove = entity;
                break;
            }
        }
        
        if(zombieToRemove != null) {
            leashEntities.remove(zombieToRemove);
            zombieToRemove.remove();
            
            // Drop lead if not distance unleashed
            if(!distanceUnleash.contains(zombieToRemove)) {
                target.getWorld().dropItemNaturally(target.getLocation(), new ItemStack(Material.LEAD));
            } else {
                distanceUnleash.remove(zombieToRemove);
            }
        }
        
        // Cancel sync task
        BukkitRunnable runnable = activeRunnables.remove(target);
        if(runnable != null) {
            runnable.cancel();
        }
        
        // Reset flight
        target.setAllowFlight(false);
        
        // Update custom player data
        CustomPlayer customTarget = CoreUtil.getPlayer(target);
        if(customTarget.getLeashedTo() != null) {
            Player leashHolder = Bukkit.getPlayer(customTarget.getLeashedTo());
            if(leashHolder != null) {
                CustomPlayer customLeashHolder = CoreUtil.getPlayer(leashHolder);
                if(customLeashHolder.getLeashedOtherPlayers() != null) {
                    customLeashHolder.getLeashedOtherPlayers().remove(target.getUniqueId());
                }
            }
            customTarget.setLeashedTo(null);
        }
    }
    
    private void recreateLeash(Player leashHolder, Player target) {
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
            zombo.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, Integer.MAX_VALUE, 255, false, false));
            zombo.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 255, false, false));
            zombo.setLeashHolder(leashHolder);
        });

        
        leashedPlayers.add(target);
        leashEntities.add(zombie);
        target.setAllowFlight(true);

        BukkitRunnable syncTask = new BukkitRunnable() {
            @Override
            public void run() {
                if(!target.isOnline() || !zombie.isValid() || !zombie.isLeashed() || !leashedPlayers.contains(target)) {
                    unleashPlayer(target);
                    cancel();
                    return;
                }

                Location targetLoc = target.getLocation();
                Location zombieLoc = zombie.getLocation();
                
                targetLoc.setX(zombieLoc.getX());
                targetLoc.setY(zombieLoc.getY());
                targetLoc.setZ(zombieLoc.getZ());
                
                target.teleport(targetLoc, PlayerTeleportEvent.TeleportCause.PLUGIN);
            }
        };
        
        syncTask.runTaskTimer(Specialization.getInstance(), 0, 1);
        activeRunnables.put(target, syncTask);
    }
}