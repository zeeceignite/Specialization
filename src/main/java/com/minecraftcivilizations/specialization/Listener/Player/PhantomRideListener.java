package com.minecraftcivilizations.specialization.Listener.Player;

import com.minecraftcivilizations.specialization.Specialization;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
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
    private final NamespacedKey lastDismountKey = new NamespacedKey(Specialization.getInstance(), "lastDismount");


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
        // Incremental growth
        AttributeInstance scaleAttr = phantom.getAttribute(Attribute.SCALE);
        if (scaleAttr != null) {
            double currentScale = scaleAttr.getBaseValue();
            if (currentScale < 1.55) {
                scaleAttr.setBaseValue(Math.min(1.55, currentScale + 0.015));
            }

            int tameGoal = 20;

            if (progress < tameGoal) {
                phantom.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, phantom.getLocation().add(0, 1, 0), 6, 0.4, 0.4, 0.4);
            } else {
                phantom.getWorld().spawnParticle(Particle.HEART, phantom.getLocation().add(0, 1, 0), 2, 0.4, 0.4, 0.4);
                phantom.setHealth(Math.min(phantom.getHealth() + 2.0, phantom.getMaxHealth()));
                phantom.setAware(false);
                phantom.setSilent(true);
                phantom.getPersistentDataContainer().set(isTamed, PersistentDataType.BYTE, (byte) 1);
                phantom.getPersistentDataContainer().set(fireResistKey, PersistentDataType.BYTE, (byte) 1);
                phantom.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, PotionEffect.INFINITE_DURATION, 0, false, false));

            }
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
        if (!canMount(phantom, 400)) return; // 400ms mount buffer

        // prevent multiple riders ===
        if (!phantom.getPassengers().isEmpty()) {
            return;
        }

        if (!player.isInsideVehicle()) {
            phantom.addPassenger(player);
            phantom.setAI(true);
            phantom.setAware(false);
            phantom.setSilent(true);
            startPhantomRide(player, phantom);
        }
    }

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

        Phantom owned = null;
        for (Phantom phantom : player.getWorld().getEntitiesByClass(Phantom.class)) {
            String ownerStr = phantom.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
            if (ownerStr != null && ownerStr.equals(uuid.toString())) {
                owned = phantom;
                break;
            }
        }

        if (owned != null) {
            // short delay ensures player is fully loaded before teleport + remount
            Phantom phantom = owned;
            BukkitRunnable rejoinTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!phantom.isValid()) return;

                    player.teleport(phantom.getLocation().add(0, 1, 0));
                    phantom.addPassenger(player);

                    phantom.setAI(true);
                    phantom.setAware(false);
                    phantom.setSilent(true);
                    phantom.getPersistentDataContainer().remove(ownerKey);

                    startPhantomRide(player, phantom);
                }
            };
            rejoinTask.runTaskLater(Specialization.getInstance(), 1L);
        }
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
        phantom.getPersistentDataContainer().remove(ownerKey);
        new BukkitRunnable() {
            private final double baseSpeed = 0.275;
            private final double maxSpeed = 1.2;
            private final double damping = 0.55; //this should slow down the player down to the base speed slowly over time
            private final double airFriction = 0.895;
            private final float maxDive = 25f;
            private final float maxClimb = 25f;
            private final float visualMaxPitch = 35f;
            private Vector velocity = new Vector(0, 0, 0);
            private double forwardEnergy = 0;
            private boolean dismounted = false;
            private double lastTargetSpeed = baseSpeed;

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
                        // Apply a small neutral buffer (no slowdown within ±5° above horizon)
                        double effectivePitch = Math.min(0, clampedPitch + 5.0); // shift upward so 0..-5° is neutral
                        double climbRatio = -effectivePitch / (maxClimb + 5.0); // normalize including buffer
                        climbRatio = Math.max(0, Math.min(1, climbRatio));

                        // Use smoother easing so slowdown only gently ramps after 5°
                        double easedClimb = Math.pow(climbRatio, 1.3);

                        // Slightly reduced slowdown factor, more lift control
                        targetSpeed = baseSpeed + easedClimb * 0.1;
                        forwardEnergy *= Math.max(0, 1 - easedClimb * 0.08);
                        dir.setY(easedClimb * 0.8);
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

                    // Smooth target speed transitions
                    double smoothedTargetSpeed = lastTargetSpeed + (targetSpeed - lastTargetSpeed) * 0.15;
                    lastTargetSpeed = smoothedTargetSpeed;

                    // Smooth forward energy transitions
                    forwardEnergy += (Math.max(0, forwardEnergy) - forwardEnergy) * 0.15;

                    // Smooth target velocity direction blending
                    Vector targetVel = dir.multiply(smoothedTargetSpeed + forwardEnergy);
                    // Apply smooth directional steering
                    velocity.add(targetVel.clone().subtract(velocity).multiply(0.25));

                    // Apply damping that pulls excess velocity back toward base speed
                    double speed = velocity.length();
                    double target = baseSpeed;
                    if (speed > target) {
                        double diff = speed - target;
                        double newSpeed = speed - diff * (1.0 - damping); // damping 0.65 = 35% decay of excess each tick
                        velocity.normalize().multiply(newSpeed);
                    }

                    // Standard air friction (minor global slowdown)
                    velocity.multiply(airFriction);


                    phantom.setVelocity(velocity);
                    phantom.setVelocity(velocity);

                    // === Speed debug display ===
                    double speedBlocksPerSec = velocity.length() * 20.0; // 20 ticks = 1 second
                    player.sendActionBar(String.format("§bSpeed: §f%.2f blocks/s", speedBlocksPerSec));

                    float visualPitch = Math.max(-visualMaxPitch, Math.min(visualMaxPitch, pitch));
                    phantom.setRotation(yaw, -visualPitch);
                    player.setFallDistance(0);


                } else {
                    if (!dismounted) {
                        phantom.setAI(true);
                        dismounted = true;
                        long now = System.currentTimeMillis();
                        phantom.getPersistentDataContainer().set(lastDismountKey, PersistentDataType.LONG, now);
                        cancel();

                    }
                }
            }
        }.runTaskTimer(Specialization.getInstance(), 0L, 1L);
    }

    private boolean canMount(Phantom phantom, long delayMs) {
        var pdc = phantom.getPersistentDataContainer();
        Long last = pdc.get(lastDismountKey, PersistentDataType.LONG);
        if (last == null) return true;
        return (System.currentTimeMillis() - last) >= delayMs;
    }
}
