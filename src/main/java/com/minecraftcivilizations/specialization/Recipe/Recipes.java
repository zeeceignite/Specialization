package com.minecraftcivilizations.specialization.Recipe;

import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.Specialization;
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
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.List;

import static com.minecraftcivilizations.specialization.Listener.Player.PlayerDeathListener.removeDownedArmorStand;

public class Recipes {

    public static void init() {
        CustomItem customItem = new CustomItem(Material.PAPER, Component.text("Bandage").color(NamedTextColor.WHITE));
        customItem.addLore(Specialization.getInstance(), List.of(
                Component.empty(),
                Component.text("Can be used to heal yourself or others.").color(NamedTextColor.WHITE)
        ));

        CustomAbility customAbility = new CustomAbility();
        customAbility.setAbilityFunction(customAbilityFunction -> {
            CustomPlayer customPlayer = (CustomPlayer) MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().getCustomPlayer(customAbilityFunction.getUniqueId());
            if (customAbilityFunction.getTargetEntity(4) instanceof Player player && player.getHealth() < player.getAttribute(Attribute.MAX_HEALTH).getValue()) {
                Player healer = Bukkit.getPlayer(customAbilityFunction.getUniqueId());
                player.heal(10);
                customPlayer.addSkillXp(SkillType.HEALER, 15);
                CustomPlayer healedPlayer = CoreUtil.getPlayer(player.getUniqueId());
                healedPlayer.setDowned(false);
                if (healer != null) {
                    healer.getInventory().getItemInMainHand().setAmount(healer.getInventory().getItemInMainHand().getAmount() - 1);
                }
                removeDownedArmorStand(player);
            }
        });
        customAbility.setCooldown(1);
        customAbility.setCastEvent(AbilityCastEvent.SNEAK_RIGHT_CLICK);
        customAbility.setName("Bandage");
        customAbility.setDescription("Bandage");

        CustomItemAbilityRegistry.register(new NamespacedKey(Specialization.getInstance(), "bandage"), customAbility);

        customItem.addAbility(new NamespacedKey(Specialization.getInstance(), "bandage"));

        CustomItemRegistry.register(new NamespacedKey(Specialization.getInstance(), "bandage"), customItem);


        ShapelessRecipe shapelessRecipe = new ShapelessRecipe(new NamespacedKey(Specialization.getInstance(), "bandage"), customItem.getItem());
        shapelessRecipe.addIngredient(8, Material.PAPER);
        shapelessRecipe.addIngredient(Material.SUGAR_CANE);
        Bukkit.addRecipe(shapelessRecipe, true);

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

        addNetherRecipes();
    }

    public static void addNetherRecipes(){
        ShapelessRecipe netheriteUpgrade = new ShapelessRecipe(new NamespacedKey(Specialization.getInstance(), "netherite_upgrade"), new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
        netheriteUpgrade.addIngredient(1, Material.NETHERITE_INGOT);
        netheriteUpgrade.addIngredient(6, Material.DIAMOND);
        netheriteUpgrade.addIngredient(1, Material.NETHER_WART_BLOCK);
        Bukkit.addRecipe(netheriteUpgrade, true);

        ShapelessRecipe blazeRod = new ShapelessRecipe(new NamespacedKey(Specialization.getInstance(), "blaze_rod"), new ItemStack(Material.BLAZE_ROD));
        blazeRod.addIngredient(1, Material.GOLD_INGOT);
        blazeRod.addIngredient(3, Material.GUNPOWDER);
        blazeRod.addIngredient(1, Material.CRIMSON_NYLIUM);
        blazeRod.addIngredient(1, Material.WARPED_NYLIUM);
        Bukkit.addRecipe(blazeRod, true);

        ShapedRecipe netherWart = new ShapedRecipe(new NamespacedKey(Specialization.getInstance(), "nether_wart"), new ItemStack(Material.NETHER_WART));
        netherWart.shape(" E ", "DDD", " B ");
        netherWart.setIngredient('E', Material.BEETROOT);
        netherWart.setIngredient('D', Material.COARSE_DIRT);
        netherWart.setIngredient('B', Material.BLAZE_POWDER);
        Bukkit.addRecipe(netherWart, true);

    }

}
