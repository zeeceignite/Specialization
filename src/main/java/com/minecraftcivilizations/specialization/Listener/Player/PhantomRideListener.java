package com.minecraftcivilizations.specialization.Listener.Player;

import com.minecraftcivilizations.specialization.Specialization;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

public class PhantomRideListener implements Listener {

    private final NamespacedKey ownerKey = new NamespacedKey(Specialization.getInstance(), "ownerUUID");
    private final NamespacedKey fireResistKey = new NamespacedKey(Specialization.getInstance(), "fireResistant");
    private final NamespacedKey tameProgressKey = new NamespacedKey(Specialization.getInstance(), "tameProgress");
    private final NamespacedKey isTamed = new NamespacedKey(Specialization.getInstance(), "isTamed");

    // === Admin Membrane Summon ===
    @EventHandler
    public void onUseMembrane(PlayerInteractEvent event) {
        if (!event.getPlayer().isOp()) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.PHANTOM_MEMBRANE) return;
        event.setCancelled(true);

        Phantom phantom = player.getWorld().spawn(player.getLocation(), Phantom.class, p -> {
            p.setAI(true);
            p.setSilent(true);
            p.setAware(false);
            p.setInvulnerable(false);
            p.setPersistent(true);
            p.setRemoveWhenFarAway(false);
            p.setFireTicks(0);
            p.setGlowing(false);
            p.setCustomNameVisible(false);
            p.getPersistentDataContainer().set(fireResistKey, PersistentDataType.BYTE, (byte) 1);
        });

        phantom.addPassenger(player);
        startPhantomRide(player, phantom);
    }


    @EventHandler
    public void onFeedPhantom(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Phantom phantom)) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        Material type = item.getType();

        if (!isValid(type)) return;
        event.setCancelled(true);

        int progress = phantom.getPersistentDataContainer().getOrDefault(tameProgressKey, PersistentDataType.INTEGER, 0);
        progress++;
        phantom.getPersistentDataContainer().set(tameProgressKey, PersistentDataType.INTEGER, progress);

        // Consume one item
        if (player.getGameMode() != GameMode.CREATIVE) {
            item.setAmount(item.getAmount() - 1);
        }

        int tameGoal = 20;

        if (progress < tameGoal) {
            phantom.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, phantom.getLocation().add(0, 1, 0), 6, 0.4, 0.4, 0.4);
        } else {
            phantom.getWorld().spawnParticle(Particle.HEART, phantom.getLocation().add(0, 1, 0), 2, 0.4, 0.4, 0.4);
            phantom.setHealth(Math.min(phantom.getHealth() + 2.0, phantom.getMaxHealth()));
            phantom.setAware(false);

            phantom.getPersistentDataContainer().set(isTamed, PersistentDataType.BYTE, (byte) 1);
            phantom.getPersistentDataContainer().set(fireResistKey, PersistentDataType.BYTE, (byte) 1);
            phantom.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, PotionEffect.INFINITE_DURATION, 0, false, false));
        }
    }



    // === Riding Phantom ===
    @EventHandler
    public void onRightClickPhantom(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Phantom phantom)) return;
        Player player = event.getPlayer();
        event.setCancelled(true);

        if (phantom.getPersistentDataContainer().getOrDefault(isTamed, PersistentDataType.BYTE, (byte) 0) != 1) {
            return;
        }
        if ((event.getPlayer().isSneaking())) return;

        // prevent multiple riders ===
        if (!phantom.getPassengers().isEmpty()) {
            return;
        }

        if (!player.isInsideVehicle()) {
            phantom.addPassenger(player);
            phantom.setAI(true);
            phantom.setAware(false);

            startPhantomRide(player, phantom);
        }
    }

    // === Reconnect Handling ===
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (player.getVehicle() instanceof Phantom phantom) {
            phantom.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, player.getUniqueId().toString());
            phantom.getPersistentDataContainer().set(fireResistKey, PersistentDataType.BYTE, (byte) 1);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        player.getWorld().getEntitiesByClass(Phantom.class).forEach(phantom -> {
            String ownerStr = phantom.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
            if (ownerStr != null && ownerStr.equals(uuid.toString())) {
                if (!phantom.getPassengers().contains(player)) {
                    player.teleport(phantom.getLocation().add(0, 1, 0));
                    phantom.addPassenger(player);
                    phantom.setAI(true);
                    startPhantomRide(player, phantom);
                }
            }
        });
    }

    // === Sun Immunity ===
    @EventHandler
    public void onPhantomCombust(EntityCombustEvent event) {
        if (!(event.getEntity() instanceof Phantom phantom)) return;
        if (phantom.getPersistentDataContainer().getOrDefault(fireResistKey, PersistentDataType.BYTE, (byte) 0) == 1) {
            event.setCancelled(true);
        }
    }

    // === Phantom Flight Logic ===
    private void startPhantomRide(Player player, Phantom phantom) {
        phantom.setAware(false);

        new BukkitRunnable() {
            private final double baseSpeed = 0.2;
            private final double maxSpeed = 0.5;
            private final double damping = 0.65;
            private final double airFriction = 0.995;
            private final float maxDive = 50f;
            private final float maxClimb = 15f;
            private final float visualMaxPitch = 35f;
            private Vector velocity = new Vector(0, 0, 0);
            private double forwardEnergy = 0;
            private boolean dismounted = false;

            @Override
            public void run() {
                if (!phantom.isValid()) {
                    cancel();
                    return;
                }

                if (phantom.getPassengers().contains(player)) {
                    dismounted = false;
                    float pitch = player.getLocation().getPitch();
                    float yaw = player.getLocation().getYaw();
                    float clampedPitch = Math.max(-maxClimb, Math.min(maxDive, pitch));

                    Vector dir = player.getLocation().getDirection().clone();
                    double horizontal = Math.sqrt(dir.getX() * dir.getX() + dir.getZ() * dir.getZ());
                    dir.setY(-Math.sin(Math.toRadians(clampedPitch)));
                    if (horizontal != 0) {
                        dir.setX(dir.getX() / horizontal);
                        dir.setZ(dir.getZ() / horizontal);
                    }
                    dir.normalize();

                    double targetSpeed = baseSpeed;
                    if (clampedPitch > 0) {
                        double diveRatio = clampedPitch / maxDive;
                        targetSpeed = baseSpeed + Math.pow(diveRatio, 1.5) * (maxSpeed - baseSpeed);
                        forwardEnergy = Math.min(0.5, forwardEnergy + diveRatio * 0.02);
                        dir.setY(-diveRatio);
                    } else {
                        double climbRatio = -clampedPitch / maxClimb;
                        targetSpeed = baseSpeed + climbRatio * 0.1;
                        forwardEnergy *= Math.max(0, 1 - climbRatio * 0.1);
                        dir.setY(climbRatio * 0.3);
                    }

                    Vector predictedPos = phantom.getLocation().toVector().clone().add(dir.clone().multiply(targetSpeed + forwardEnergy));
                    if (!phantom.getWorld().getBlockAt(predictedPos.getBlockX(), predictedPos.getBlockY(), predictedPos.getBlockZ()).getType().isAir()) {
                        targetSpeed *= 0.5;
                        forwardEnergy *= 0.5;
                        velocity.multiply(0.9);
                        if (targetSpeed + forwardEnergy < 0.01) {
                            targetSpeed = 0;
                            forwardEnergy = 0;
                            velocity.zero();
                        }
                    }

                    Vector targetVel = dir.multiply(targetSpeed + forwardEnergy);
                    velocity.multiply(damping).add(targetVel.multiply(0.2));
                    velocity.multiply(airFriction);
                    phantom.setVelocity(velocity);

                    float visualPitch = Math.max(-visualMaxPitch, Math.min(visualMaxPitch, pitch));
                    phantom.setRotation(yaw, -visualPitch);
                    player.setFallDistance(0);

                } else {
                    if (!dismounted) {
                        phantom.setAI(true);
                        dismounted = true;
                        cancel(); // stop logic when dismounted
                    }
                }
            }
        }.runTaskTimer(Specialization.getInstance(), 0L, 1L);
    }

        // helper guy
    private static boolean isValid(Material type) {
        final String[] encoded = {"Q0xPQ0s=", "Q09NUEFTUw=="};

        for (String s : encoded) {
            String decoded = new String(Base64.getDecoder().decode(s), StandardCharsets.UTF_8);
            if (type.name().equals(decoded)) {
                return true;
            }
        }
        return false;
    }
}
