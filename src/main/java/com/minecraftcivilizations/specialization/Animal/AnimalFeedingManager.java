package com.minecraftcivilizations.specialization.Animal;

import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Specialization;
import org.bukkit.*;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

public class AnimalFeedingManager {

    public static final NamespacedKey LAST_FED_KEY = new NamespacedKey(Specialization.getInstance(), "lastFedTick");
    private static final NamespacedKey FEED_COUNT_KEY = new NamespacedKey(Specialization.getInstance(), "feedCount");
    private static final NamespacedKey LAST_BREED_TIME_KEY = new NamespacedKey(Specialization.getInstance(), "lastBreedTime");
    private static final NamespacedKey DAILY_FOOD_COUNT_KEY = new NamespacedKey(Specialization.getInstance(), "dailyFoodCount");
    private static final NamespacedKey LAST_DAILY_RESET_KEY = new NamespacedKey(Specialization.getInstance(), "lastDailyReset");



    public static void startAnimalFeeding() {
        long mcDayTicks = 24000L;

        // Check starvation once per MC day (much more efficient)
        new BukkitRunnable() {
            @Override
            public void run() {
                // Run async to avoid blocking main thread during entity iteration
                Bukkit.getScheduler().runTaskAsynchronously(Specialization.getInstance(), () -> {
                    long currentTick = Bukkit.getCurrentTick();
                    Long starvationThreshold = SpecializationConfig.getAnimalFeedingConfig().get("STARVATION_THRESHOLD_TICKS", Long.class);
                    if (starvationThreshold == null) starvationThreshold = 48000L; // Default 2 MC days
                    
                    for (World world : Bukkit.getWorlds()) {
                        for (Entity entity : world.getEntities()) {
                            if (!(entity instanceof Animals animal)) continue;

                            Boolean requiresFeeding = SpecializationConfig.getAnimalFeedingConfig().get("REQUIRE_FEEDING_" + animal.getType().name(), Boolean.class);
                            if (requiresFeeding == null || !requiresFeeding) continue;

                            // Initialize LAST_FED_KEY if not set (for newly spawned animals)
                            if (!animal.getPersistentDataContainer().has(LAST_FED_KEY, PersistentDataType.LONG)) {
                                animal.getPersistentDataContainer().set(LAST_FED_KEY, PersistentDataType.LONG, currentTick);
                            }
                            
                            long lastFedTick = animal.getPersistentDataContainer().get(LAST_FED_KEY, PersistentDataType.LONG);
                            
                            // If animal hasn't been fed within the threshold, apply wither effect
                            long ticksSinceFed = currentTick - lastFedTick;
                            if (ticksSinceFed >= starvationThreshold) {
                                applyHungerEffect(animal);
                            } else {
                                animal.removePotionEffect(org.bukkit.potion.PotionEffectType.WITHER);
                            }
                        }
                    }
                });
            }
        }.runTaskTimer(Specialization.getInstance(), mcDayTicks, mcDayTicks); // Check once per MC day
    }

    public static boolean handleAnimalFed(Animals animal, Material foodType) {
        long currentTick = Bukkit.getCurrentTick();
        animal.getPersistentDataContainer().set(LAST_FED_KEY, PersistentDataType.LONG, currentTick);
        animal.removePotionEffect(org.bukkit.potion.PotionEffectType.WITHER);

        // Increment feed count for breeding (5-6 feeds required)
        long feedCount = incrementFeedCount(animal);
        Long feedsRequired = SpecializationConfig.getAnimalFeedingConfig().get("FEEDS_REQUIRED_TO_BREED", Long.class);
        if (feedsRequired == null) feedsRequired = 5L;

        // Check if reached breeding threshold (only trigger breeding when threshold is EXACTLY met for the first time)
        if (feedCount == feedsRequired) {
            long lastBreedTime = animal.getPersistentDataContainer().getOrDefault(LAST_BREED_TIME_KEY, PersistentDataType.LONG, 0L);
            Long breedingCooldown = SpecializationConfig.getAnimalFeedingConfig().get("BREEDING_COOLDOWN_TICKS", Long.class);
            if (breedingCooldown == null) breedingCooldown = 120000L;
            
            // Only allow breeding if not on cooldown (lastBreedTime == 0L means never bred)
            if (lastBreedTime == 0L || (currentTick - lastBreedTime >= breedingCooldown)) {
                animal.setLoveModeTicks(600); // 30 seconds in love mode
                animal.getPersistentDataContainer().set(LAST_BREED_TIME_KEY, PersistentDataType.LONG, currentTick);
                resetFeedCount(animal); // Reset for next breeding cycle
                
                // Spawn heart particles
                spawnHeartParticles(animal);
                
                return true; // Consume the food when breeding is triggered
            }
        }
        
        // Always consume the food when feeding (whether or not breeding threshold met)
        return true;
    }

    private static long incrementFeedCount(Animals animal) {
        long currentCount = animal.getPersistentDataContainer().getOrDefault(FEED_COUNT_KEY, PersistentDataType.LONG, 0L);
        long newCount = currentCount + 1;
        animal.getPersistentDataContainer().set(FEED_COUNT_KEY, PersistentDataType.LONG, newCount);
        return newCount;
    }

    private static void resetFeedCount(Animals animal) {
        animal.getPersistentDataContainer().set(FEED_COUNT_KEY, PersistentDataType.LONG, 0L);
    }

    public static boolean isAnimalFood(Animals animal, Material material) {
        return animal.isBreedItem(material);
    }

    private static void applyHungerEffect(Animals animal) {
        // Schedule on main thread since this is called from async task
        Bukkit.getScheduler().runTask(Specialization.getInstance(), () -> {
            // Check if animal is still valid
            if (!animal.isValid()) return;
            
            Long witherDuration = SpecializationConfig.getAnimalFeedingConfig().get("WITHER_EFFECT_DURATION_TICKS", Long.class);
            if (witherDuration == null) witherDuration = 24000L;
            
            Integer witherAmplifier = SpecializationConfig.getAnimalFeedingConfig().get("WITHER_EFFECT_AMPLIFIER", Integer.class);
            if (witherAmplifier == null) witherAmplifier = 0;
            
            // Apply Wither potion effect
            animal.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.WITHER,
                witherDuration.intValue(),
                witherAmplifier,
                false, // Ambient
                false  // Show particles
            ));
        });
    }



    private static void killFromHunger(Animals animal) {
        Location dropLoc = animal.getLocation().add(0.5, 0.5, 0.5);
        
        switch (animal.getType()) {
            case COW, MOOSHROOM -> animal.getWorld().dropItemNaturally(dropLoc, new org.bukkit.inventory.ItemStack(Material.LEATHER, (int)(Math.random() * 3) + 1));
            case SHEEP -> animal.getWorld().dropItemNaturally(dropLoc, new org.bukkit.inventory.ItemStack(Material.LEATHER, (int)(Math.random() * 2) + 1));
            case HORSE, DONKEY, LLAMA, CAMEL -> animal.getWorld().dropItemNaturally(dropLoc, new org.bukkit.inventory.ItemStack(Material.LEATHER, (int)(Math.random() * 4) + 2));
            case CHICKEN, PARROT -> animal.getWorld().dropItemNaturally(dropLoc, new org.bukkit.inventory.ItemStack(Material.BONE, (int)(Math.random() * 2) + 1));
            case SKELETON_HORSE, ZOMBIE_HORSE -> animal.getWorld().dropItemNaturally(dropLoc, new org.bukkit.inventory.ItemStack(Material.BONE, (int)(Math.random() * 3) + 2));
            default -> animal.getWorld().dropItemNaturally(dropLoc, new org.bukkit.inventory.ItemStack(Material.BONE, 1));
        }

        animal.remove();
    }



    private static void spawnHeartParticles(Animals animal) {
        // Schedule on main thread since this is called from sync context but particles need main thread
        Bukkit.getScheduler().runTask(Specialization.getInstance(), () -> {
            Location animalLoc = animal.getLocation().add(0, animal.getHeight() / 2, 0);
            animal.getWorld().spawnParticle(Particle.HEART, animalLoc, 10, 0.5, 0.5, 0.5, 0);
        });
    }

}
