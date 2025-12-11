package com.minecraftcivilizations.specialization.Food;

import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Specialization;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Objects;
import java.util.Random;

public class FoodRotManager {

    public static final NamespacedKey CREATION_TIME_KEY = new NamespacedKey(Specialization.getInstance(), "foodCreationTime");
    public static final NamespacedKey EXPIRY_TIME_KEY = new NamespacedKey(Specialization.getInstance(), "foodExpiryTime");
    public static final NamespacedKey BLESSED_FOOD_KEY = new NamespacedKey(Specialization.getInstance(), "BLESSED_FOOD");
    public static final NamespacedKey SMOKED_FOOD_KEY = new NamespacedKey(Specialization.getInstance(), "foodSmoked");

    private static double getFoodSaturation(Material foodType) {
        try {
            ItemStack temp = new ItemStack(foodType);
            if (temp.hasItemMeta()) {
                var foodComponent = temp.getItemMeta().getFood();
                if (foodComponent != null) {
                    float saturation = foodComponent.getSaturation();
                    return Math.min(5.0, saturation);
                }
            }
        } catch (Exception e) {

        }
        return 2.0;
    }
    
    /**
     * Calculate rot duration based on food's saturation value
     * Duration in minutes = (maxSaturation - saturation + 1) * multiplier
     * Lower saturation = longer shelf life (slower expiration)
     * Higher saturation = shorter shelf life (faster expiration)
     * Blessed food gets a duration multiplier
     */
    public static long calculateRotDurationForFood(ItemStack food) {
        if (food == null) return 0;
        
        Double minuteMultiplier = SpecializationConfig.getFoodRotConfig().get("FOOD_ROT_MINUTES_PER_SATURATION", Double.class);
        if (minuteMultiplier == null) minuteMultiplier = 15.0;
        
        double foodSaturation = getFoodSaturation(food.getType());
        double maxSaturation = 5.0;
        double durationMinutes = (maxSaturation - foodSaturation + 1) * minuteMultiplier;
        if (isBlessedFood(food)) {
            Double blessingMultiplier = SpecializationConfig.getFoodRotConfig().get("BLESSED_FOOD_DURATION_MULTIPLIER", Double.class);
            if (blessingMultiplier == null) blessingMultiplier = 2.0;
            durationMinutes *= blessingMultiplier;
        }

        if (isSmokedFood(food)) {
            durationMinutes *= 2.0;
        }

        return (long) (durationMinutes * 60 * 20);
    }

    public static void stampFoodCreationTime(ItemStack food) {
        if (food == null || !isFood(food)) return;

        Boolean enabled = SpecializationConfig.getFoodRotConfig().get("FOOD_ROT_ENABLED", Boolean.class);
        if (enabled == null || !enabled) return;

        ItemMeta meta = food.getItemMeta();
        if (meta == null) return;

        long currentGameTick = getCurrentGameTick();
        long rotDurationTicks = calculateRotDurationForFood(food);
        long expiryTime = currentGameTick + rotDurationTicks;
        
        meta.getPersistentDataContainer().set(CREATION_TIME_KEY, PersistentDataType.LONG, currentGameTick);
        meta.getPersistentDataContainer().set(EXPIRY_TIME_KEY, PersistentDataType.LONG, expiryTime);
        food.setItemMeta(meta);
        updateExpirationLore(food);
    }



    public static void checkFoodRotOnConsume(ItemStack consumed, Player player) {
        if (consumed == null || !isFood(consumed) || player == null) return;

        Boolean enabled = SpecializationConfig.getFoodRotConfig().get("FOOD_ROT_ENABLED", Boolean.class);
        if (enabled == null || !enabled) return;

        ItemMeta meta = consumed.getItemMeta();
        if (meta == null) return;

        Long creationTime = meta.getPersistentDataContainer().get(CREATION_TIME_KEY, PersistentDataType.LONG);
        if (creationTime == null) {
            stampFoodCreationTime(consumed);
            return;
        }
        updateExpirationLore(consumed);

        long currentGameTick = getCurrentGameTick();
        long rotDurationTicks = calculateRotDurationForFood(consumed);
        long expiryTime = creationTime + rotDurationTicks;
        
        boolean hasExpired = currentGameTick > expiryTime;

        if (hasExpired) {
            if (!isBlessedFood(consumed)) {
                applyPoisonEffect(player);
            }
            addRotWarningToLore(consumed);
        }
    }

    private static void applyPoisonEffect(Player player) {
        Double poisonChance = SpecializationConfig.getFoodRotConfig().get("POISON_CHANCE_ON_ROT", Double.class);
        if (poisonChance == null) poisonChance = 0.15;

        if (new Random().nextDouble() < poisonChance) {
            Long durationTicks = SpecializationConfig.getFoodRotConfig().get("POISON_DURATION_TICKS", Long.class);
            Long amplifier = SpecializationConfig.getFoodRotConfig().get("POISON_AMPLIFIER", Long.class);
            
            if (durationTicks == null) durationTicks = 120L;
            if (amplifier == null) amplifier = 1L;

            player.addPotionEffect(new PotionEffect(
                PotionEffectType.POISON,
                durationTicks.intValue(),
                amplifier.intValue(),
                false,
                true
            ));
            
            player.sendMessage(ChatColor.DARK_RED + "Ugh! The rotten food made you sick...");
        }
    }

    private static void addRotWarningToLore(ItemStack food) {
        ItemMeta meta = food.getItemMeta();
        if (meta == null) return;

        List<String> lore = meta.getLore();
        if (lore != null && lore.stream().anyMatch(line -> ChatColor.stripColor(line).contains("Rotten"))) {
            return;
        }

        if (lore == null) {
            lore = new java.util.ArrayList<>();
        } else {
            lore = new java.util.ArrayList<>(lore);
        }

        lore.add(ChatColor.DARK_RED + "§m§n" + ChatColor.DARK_RED + " Rotten " + ChatColor.DARK_RED);
        meta.setLore(lore);
        food.setItemMeta(meta);
    }

    /**
     * Update expiration info in food lore (shows time until expiration)
     * Duration varies by food saturation: higher saturation = shorter shelf life
     * Blessed food has extended duration
     */
    public static void updateExpirationLore(ItemStack food) {
        if (food == null) return;

        Boolean showExpiration = SpecializationConfig.getFoodRotConfig().get("SHOW_EXPIRATION_IN_LORE", Boolean.class);
        if (showExpiration == null || !showExpiration) return;

        ItemMeta meta = food.getItemMeta();
        if (meta == null) return;

        Long creationTime = meta.getPersistentDataContainer().get(CREATION_TIME_KEY, PersistentDataType.LONG);
        if (creationTime == null) {
            stampFoodCreationTime(food);
            return;
        }

        long rotDurationTicks = calculateRotDurationForFood(food);
        long currentGameTick = getCurrentGameTick();
        long expiryTime = creationTime + rotDurationTicks;
        long remainingTicks = expiryTime - currentGameTick;
        long remainingMs = remainingTicks * 50;

        String expirationLine;
        if (remainingMs <= 0) {
            expirationLine = ChatColor.DARK_RED + "Expired";
        } else {
            String timeLeft = formatTime(remainingMs);
            expirationLine = ChatColor.DARK_GRAY + "Expires in: " + ChatColor.GRAY + timeLeft;
        }

        List<String> lore = meta.getLore();
        if (lore == null) {
            lore = new java.util.ArrayList<>();
        } else {
            lore = new java.util.ArrayList<>(lore);
        }

        lore.removeIf(line -> ChatColor.stripColor(line).contains("Expires") || ChatColor.stripColor(line).equals("Expired"));
        if (!lore.stream().anyMatch(line -> ChatColor.stripColor(line).equals("Expired"))) {
            lore.add(expirationLine);
        }

        if (isBlessedFood(food)) {
            lore.removeIf(line -> ChatColor.stripColor(line).contains("Blessed"));
            lore.add(ChatColor.GOLD + "Blessed Food");
        }
        
        meta.setLore(lore);
        food.setItemMeta(meta);
    }

    private static String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            long remainingMinutes = minutes % 60;
            return hours + "h " + remainingMinutes + "m";
        } else if (minutes > 0) {
            long remainingSeconds = seconds % 60;
            return minutes + "m " + remainingSeconds + "s";
        } else {
            return seconds + "s";
        }
    }

    public static long getFoodAgeSeconds(ItemStack food) {
        if (food == null) return -1;

        ItemMeta meta = food.getItemMeta();
        if (meta == null) return -1;

        Long creationGameTick = meta.getPersistentDataContainer().get(CREATION_TIME_KEY, PersistentDataType.LONG);
        if (creationGameTick == null) return 0;

        long currentGameTick = getCurrentGameTick();
        long ageInTicks = currentGameTick - creationGameTick;
        return (ageInTicks * 50) / 1000;
    }

    /**
     * Update all food items in a player's inventory (both main and armor)
     * Stamps any food that doesn't have a creation time
     */
    public static void updateInventoryFoodLore(Player player) {
        if (player == null) return;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType().isEdible()) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    Long creationTime = meta.getPersistentDataContainer().get(CREATION_TIME_KEY, PersistentDataType.LONG);
                    if (creationTime == null) {
                        stampFoodCreationTime(item);
                    }
                }
                updateExpirationLore(item);
            }
        }
    }

    /**
     * Get the current game tick from the server
     * Respects /tick commands for tick speed manipulation
     */
    private static long getCurrentGameTick() {
        World world = Bukkit.getWorlds().get(0);
        return world != null ? world.getFullTime() : 0;
    }

    private static boolean isFood(ItemStack item) {
        if (item == null) return false;
        return item.getType().isEdible();
    }

    private static boolean isBlessedFood(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return false;
        return item.getItemMeta().getPersistentDataContainer().has(BLESSED_FOOD_KEY, PersistentDataType.BOOLEAN);
    }

    private static boolean isSmokedFood(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return false;
        return item.getItemMeta().getPersistentDataContainer().has(SMOKED_FOOD_KEY, PersistentDataType.BOOLEAN);
    }

    public static void markFoodAsSmoked(ItemStack food) {
        if (food == null || !isFood(food)) return;
        
        ItemMeta meta = food.getItemMeta();
        if (meta == null) return;
        
        meta.getPersistentDataContainer().set(SMOKED_FOOD_KEY, PersistentDataType.BOOLEAN, true);
        food.setItemMeta(meta);
    }

    /**
     * Mark food as blessed (extends rot duration)
     */
    public static void blessFoodItem(ItemStack food) {
        if (food == null || !isFood(food)) return;
        
        ItemMeta meta = food.getItemMeta();
        if (meta == null) return;
        
        meta.getPersistentDataContainer().set(BLESSED_FOOD_KEY, PersistentDataType.BOOLEAN, true);
        food.setItemMeta(meta);
        

        updateExpirationLore(food);
    }

    public static boolean isBlessed(ItemStack food) {
        return isBlessedFood(food);
    }
}
