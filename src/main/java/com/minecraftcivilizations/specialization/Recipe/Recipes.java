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
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class Recipes {

    public static void init() {
        ItemStack itemStack = ItemStack.of(Material.PAPER);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(Component.text("Bandage").color(NamedTextColor.WHITE));
        itemStack.setItemMeta(itemMeta);

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

        CustomItem newCustomItem = new CustomItem();
        newCustomItem.setItem(itemStack);
        newCustomItem.addLore(List.of(Component.empty(),
                Component.text("Can be used to heal yourself or others.").color(NamedTextColor.WHITE)));
        newCustomItem.addAbility(new NamespacedKey(Specialization.getInstance(), "bandage"));

        CustomItemRegistry.register(new NamespacedKey(Specialization.getInstance(), "bandage"), newCustomItem);


        ShapelessRecipe shapelessRecipe = new ShapelessRecipe(new NamespacedKey(Specialization.getInstance(), "bandage"), itemStack);
        shapelessRecipe.addIngredient(new ItemStack(Material.PAPER, 8));
        shapelessRecipe.addIngredient(new ItemStack(Material.SUGAR_CANE));
        Bukkit.addRecipe(shapelessRecipe, true);
    }
}
