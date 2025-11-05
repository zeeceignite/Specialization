package com.minecraftcivilizations.specialization.Recipe;

import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.Specialization;
import com.minecraftcivilizations.specialization.StaffTools.Debug;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import minecraftcivilizations.com.minecraftCivilizationsCore.Ability.AbilityCastEvent;
import minecraftcivilizations.com.minecraftCivilizationsCore.Ability.CustomAbility;
import minecraftcivilizations.com.minecraftCivilizationsCore.Ability.CustomItemAbilityRegistry;
import minecraftcivilizations.com.minecraftCivilizationsCore.Item.CustomItem;
import minecraftcivilizations.com.minecraftCivilizationsCore.Item.CustomItemRegistry;
import minecraftcivilizations.com.minecraftCivilizationsCore.MinecraftCivilizationsCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.List;

import static com.minecraftcivilizations.specialization.Listener.Player.PlayerDeathListener.removeDownedArmorStand;

public class Recipes {

    public static void init() {
        registerCustomItems();
        registerRecipes(false);
        startPeriodicRecipeRefresh(); //temporary patch to re-register minecraft:rail
    }
    private static void registerCustomItems() {
        CustomItem customItem = new CustomItem(Material.PAPER, Component.text("Bandage").color(NamedTextColor.WHITE));
        customItem.addLore(Specialization.getInstance(), List.of(
                Component.text("Shift + Right Click another player to heal.").color(NamedTextColor.BLUE),
                Component.empty(),
                Component.text("Amount Healed and XP gained scale with Healer level.").color(NamedTextColor.GRAY)
        ));

        // Add enchantment glint (visual only)
        ItemStack item = customItem.getItem();
        var meta = item.getItemMeta();
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        customItem.setItem(item);

        CustomAbility customAbility = new CustomAbility();
        customAbility.setAbilityFunction(customAbilityFunction -> {
            CustomPlayer cHealer = (CustomPlayer) MinecraftCivilizationsCore.getInstance()
                    .getCustomPlayerManager().getCustomPlayer(customAbilityFunction.getUniqueId());
            if (cHealer.getSkillLevel(SkillType.HEALER) == 0) return;

            Player healer = Bukkit.getPlayer(customAbilityFunction.getUniqueId());
            if (healer == null || healer.getFoodLevel() < 3) return;

            Player target = null;
            if (customAbilityFunction.getTargetEntity(4) instanceof Player p)
                target = p;
            else
                target = healer; // fallback to self-heal

            if (target.getHealth() >= target.getAttribute(Attribute.MAX_HEALTH).getValue()) return;

            int level = Math.min(cHealer.getSkillLevel(SkillType.HEALER), 5);
            double healAmount = 2 + ((level - 1) * (8.0 / 4.0)); // 2→10
            int xp = 15 + (int) ((level - 1) * (35.0 / 4.0));    // 15→50

            healer.setFoodLevel(healer.getFoodLevel() - 3);
            target.setHealth(Math.min(target.getHealth() + healAmount, target.getAttribute(Attribute.MAX_HEALTH).getValue()));
            cHealer.addSkillXp(SkillType.HEALER, xp);

            CustomPlayer healedPlayer = CoreUtil.getPlayer(target.getUniqueId());
            healedPlayer.setDowned(false);

            ItemStack hand = healer.getInventory().getItemInMainHand();
            hand.setAmount(hand.getAmount() - 1);
            healer.getInventory().setItemInMainHand(hand);

            removeDownedArmorStand(target);
        });
        customAbility.setCooldown(1);
        customAbility.setCastEvent(AbilityCastEvent.SNEAK_RIGHT_CLICK);
        customAbility.setName("Bandage");

        NamespacedKey bandageKey = new NamespacedKey(Specialization.getInstance(), "bandage");
        CustomItemAbilityRegistry.register(bandageKey, customAbility);
        customItem.addAbility(bandageKey);
        CustomItemRegistry.register(bandageKey, customItem);
    }


    public static void registerRecipes(boolean reloading) {
        String register_mode = (reloading?"Re-registered":"Registered");
        Bukkit.getLogger().info("[Recipes] "+(reloading?"Re-registering":"registering")+" custom recipes...");
        int successCount = 0;
        int failCount = 0;
        for (NamespacedKey key : CustomItemRegistry.getItems().keySet()) {
            CustomItem customItem = CustomItemRegistry.getItem(key);
            if (customItem != null) {
                try {
                    ShapelessRecipe shapelessRecipe = new ShapelessRecipe(key, customItem.getItem());
                    if (key.getKey().equals("bandage")) {
                        shapelessRecipe.addIngredient(8, Material.PAPER);
                        shapelessRecipe.addIngredient(Material.SUGAR_CANE);
                    }
                    Bukkit.addRecipe(shapelessRecipe, true);
                    Bukkit.getLogger().info("[Recipes] ✓ "+register_mode+" recipe: " + key);
                    successCount++;
                } catch (Exception e) {
                    if(!reloading)
                        Bukkit.getLogger().warning("[Recipes] ✗ Failed to "+register_mode+" recipe: " + key + " - " + e.getMessage());
                    failCount++;
                }
            }
        }

        try {
            ShapedRecipe shapedRecipe = new ShapedRecipe(NamespacedKey.minecraft("rail"), new ItemStack(Material.RAIL).add(64));
            shapedRecipe.shape(
                    "I I",
                    "ISI",
                    "I I"
            );
            shapedRecipe.setIngredient('I', Material.IRON_INGOT);
            shapedRecipe.setIngredient('S', Material.STICK);

            Bukkit.removeRecipe(NamespacedKey.minecraft("rail"));
            Bukkit.addRecipe(shapedRecipe);
            Bukkit.getLogger().info("[Recipes] ✓ "+register_mode+" modified recipe: minecraft:rail");
            successCount++;
        } catch (Exception e) {
            if(!reloading)
                Bukkit.getLogger().warning("[Recipes] ✗ Failed to "+register_mode+" rail recipe - " + e.getMessage());
            failCount++;
        }

        int netherCount = addNetherRecipes(reloading);

        try {
            addUnobtainableRecipes();
        } catch (Exception ignored){
            // recipe already added
        }

        successCount += netherCount;

        Bukkit.getLogger().info("[Recipes] Registration complete: " + successCount + " successful, " + failCount + " failed");
    }

    private static void startPeriodicRecipeRefresh() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(Specialization.getInstance(), () -> {
            Bukkit.getLogger().info("[Recipes] Periodic recipe refresh triggered");
            Debug.broadcast("recipes", "Periodic recipe refresh triggered", null, true);
            Bukkit.getScheduler().runTask(Specialization.getInstance(), () -> Recipes.registerRecipes(true));
        }, 1200L, 1200L);
    }

    public static int addNetherRecipes(boolean reloading) {
        int count = 0;

        try {
            ShapelessRecipe netheriteUpgrade = new ShapelessRecipe(new NamespacedKey(Specialization.getInstance(), "netherite_upgrade"), new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
            netheriteUpgrade.addIngredient(1, Material.NETHERITE_INGOT);
            netheriteUpgrade.addIngredient(6, Material.DIAMOND);
            netheriteUpgrade.addIngredient(1, Material.NETHER_WART_BLOCK);
            Bukkit.addRecipe(netheriteUpgrade, true);
            Bukkit.getLogger().info("[Recipes] ✓ Registered recipe: specialization:netherite_upgrade");
            count++;
        } catch (Exception e) {
            if(!reloading)
                Bukkit.getLogger().warning("[Recipes] ✗ Failed to register netherite_upgrade - " + e.getMessage());
        }

        try {
            ShapelessRecipe blazeRod = new ShapelessRecipe(new NamespacedKey(Specialization.getInstance(), "blaze_rod"), new ItemStack(Material.BLAZE_ROD));
            blazeRod.addIngredient(1, Material.GOLD_INGOT);
            blazeRod.addIngredient(3, Material.GUNPOWDER);
            blazeRod.addIngredient(1, Material.CRIMSON_NYLIUM);
            blazeRod.addIngredient(1, Material.WARPED_NYLIUM);
            Bukkit.addRecipe(blazeRod, true);
            Bukkit.getLogger().info("[Recipes] ✓ Registered recipe: specialization:blaze_rod");
            count++;
        } catch (Exception e) {
            if(!reloading)
                Bukkit.getLogger().warning("[Recipes] ✗ Failed to register blaze_rod - " + e.getMessage());
        }

        try {
            ShapedRecipe netherWart = new ShapedRecipe(new NamespacedKey(Specialization.getInstance(), "nether_wart"), new ItemStack(Material.NETHER_WART));
            netherWart.shape(" E ", "DDD", " B ");
            netherWart.setIngredient('E', Material.BEETROOT);
            netherWart.setIngredient('D', Material.COARSE_DIRT);
            netherWart.setIngredient('B', Material.BLAZE_POWDER);
            Bukkit.addRecipe(netherWart, true);
            Bukkit.getLogger().info("[Recipes] ✓ Registered recipe: specialization:nether_wart");
            count++;
        } catch (Exception e) {
            if(!reloading)
                Bukkit.getLogger().warning("[Recipes] ✗ Failed to register nether_wart - " + e.getMessage());
        }

        return count;
    }

    public static void addUnobtainableRecipes(){
        ShapedRecipe catEgg = new ShapedRecipe(new NamespacedKey(Specialization.getInstance(), "cat_spawn_egg"), new ItemStack(Material.CAT_SPAWN_EGG));
        catEgg.shape("FFF", " E ", "FDF");
        catEgg.setIngredient('F', Material.TROPICAL_FISH);
        catEgg.setIngredient('E', Material.EGG);
        catEgg.setIngredient('D', Material.DIAMOND);
        Bukkit.addRecipe(catEgg, true);

        ShapedRecipe bell = new ShapedRecipe(new NamespacedKey(Specialization.getInstance(), "bell"), new ItemStack(Material.BELL));
        bell.shape(" W ", "GGG", "GGG");
        bell.setIngredient('W', new RecipeChoice.MaterialChoice(Tag.PLANKS));
        bell.setIngredient('G', Material.GOLD_INGOT);
    }
}
