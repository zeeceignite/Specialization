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

    public static void startAnimalFeeding() {
        Long tempThreshold = SpecializationConfig.getAnimalFeedingConfig().get("STARVATION_THRESHOLD_TICKS", Long.class);
        if (tempThreshold == null) tempThreshold = 48000L;
        final long starvationThreshold = tempThreshold;

        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.getScheduler().runTaskAsynchronously(Specialization.getInstance(), () -> {
                    long currentTick = Bukkit.getCurrentTick();
                    
                    Long breedingCooldown = SpecializationConfig.getAnimalFeedingConfig().get("BREEDING_COOLDOWN_TICKS", Long.class);
                    if (breedingCooldown == null) breedingCooldown = 120000L;
                    
                    for (World world : Bukkit.getWorlds()) {
                        for (Entity entity : world.getEntities()) {
                            if (!(entity instanceof Animals animal)) continue;

                            Boolean requiresFeeding = SpecializationConfig.getAnimalFeedingConfig().get("REQUIRE_FEEDING_" + animal.getType().name(), Boolean.class);
                            if (requiresFeeding == null || !requiresFeeding) continue;

                            if (!animal.isAdult()) continue;

                            long lastBreedTime = animal.getPersistentDataContainer().getOrDefault(LAST_BREED_TIME_KEY, PersistentDataType.LONG, 0L);
                            if (lastBreedTime != 0L && (currentTick - lastBreedTime < breedingCooldown)) {
                                continue;
                            }
                            
                            applyHungerEffect(animal);
                        }
                    }
                });
            }
        }.runTaskTimer(Specialization.getInstance(), starvationThreshold, starvationThreshold);
    }

    public static boolean handleAnimalFed(Animals animal, Material foodType) {
        long currentTick = Bukkit.getCurrentTick();
        long lastBreedTime = animal.getPersistentDataContainer().getOrDefault(LAST_BREED_TIME_KEY, PersistentDataType.LONG, 0L);
        Long breedingCooldown = SpecializationConfig.getAnimalFeedingConfig().get("BREEDING_COOLDOWN_TICKS", Long.class);
        if (breedingCooldown == null) breedingCooldown = 120000L;
        if (lastBreedTime != 0L && (currentTick - lastBreedTime < breedingCooldown)) {
            animal.getPersistentDataContainer().set(LAST_FED_KEY, PersistentDataType.LONG, currentTick);
            return false;
        }
        animal.getPersistentDataContainer().set(LAST_FED_KEY, PersistentDataType.LONG, currentTick);
        animal.removePotionEffect(org.bukkit.potion.PotionEffectType.WITHER);
        long feedCount = incrementFeedCount(animal);
        Long feedsRequired = SpecializationConfig.getAnimalFeedingConfig().get("FEEDS_REQUIRED_TO_BREED", Long.class);
        if (feedsRequired == null) feedsRequired = 5L;
        if (feedCount == feedsRequired) {
            animal.setLoveModeTicks(600);
            animal.getPersistentDataContainer().set(LAST_BREED_TIME_KEY, PersistentDataType.LONG, currentTick);
            resetFeedCount(animal);
            spawnHeartParticles(animal);
            
            return true;
        }
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
        Bukkit.getScheduler().runTask(Specialization.getInstance(), () -> {
            if (!animal.isValid()) return;
            
            // Apply Wither I effect (infinite duration, gets removed when fed)
            animal.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.WITHER,
                Integer.MAX_VALUE,
                0,
                false,
                false
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
        Bukkit.getScheduler().runTask(Specialization.getInstance(), () -> {
            Location animalLoc = animal.getLocation().add(0, animal.getHeight() / 2, 0);
            animal.getWorld().spawnParticle(Particle.HEART, animalLoc, 10, 0.5, 0.5, 0.5, 0);
        });
    }

}
