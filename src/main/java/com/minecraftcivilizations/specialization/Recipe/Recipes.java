package com.minecraftcivilizations.specialization.Recipe;

import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.Specialization;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class Recipes {

    public static void init() {
        CustomItem customItem = new CustomItem(Material.PAPER, Component.text("Bandage").color(NamedTextColor.WHITE),
                Component.empty(),
                Component.text("Can be used to heal yourself or others.").color(NamedTextColor.WHITE)
        );

        CustomAbility customAbility = new CustomAbility();
        customAbility.setAbilityFunction(customAbilityFunction -> {
            CustomPlayer customPlayer = (CustomPlayer) MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().getCustomPlayer(customAbilityFunction.getUniqueId());
            customPlayer.addSkillXp(SkillType.HEALER, 100);
            Specialization.logger.info("TESTING");
        });
        customAbility.setCooldown(10);
        customAbility.setCastEvent(AbilityCastEvent.RIGHT_CLICK);
        customAbility.setName("Bandage");
        customAbility.setDescription("Bandage");

        CustomItemAbilityRegistry.register(new NamespacedKey(Specialization.getInstance(), "bandage"), customAbility);

        customItem.addAbility(new NamespacedKey(Specialization.getInstance(), "bandage"));

        CustomItemRegistry.register(new NamespacedKey(Specialization.getInstance(), "bandage"), customItem);


        ShapelessRecipe shapelessRecipe = new ShapelessRecipe(new NamespacedKey(Specialization.getInstance(), "bandage"), customItem.getItem());
        shapelessRecipe.addIngredient(new ItemStack(Material.PAPER, 8));
        shapelessRecipe.addIngredient(new ItemStack(Material.SUGAR_CANE));
        Bukkit.addRecipe(shapelessRecipe, true);

        ShapedRecipe shapedRecipe = new ShapedRecipe(NamespacedKey.minecraft("rail"), new ItemStack(Material.RAIL).add(23));
        shapedRecipe.shape(
                "I I",
                "ISI",
                "I I"
        );
        shapedRecipe.setIngredient('I', Material.IRON_INGOT);
        shapedRecipe.setIngredient('S', Material.STICK);
        Bukkit.removeRecipe(NamespacedKey.minecraft("rail"));
        Bukkit.addRecipe(shapedRecipe);
    }
}
