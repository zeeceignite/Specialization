package com.minecraftcivilizations.specialization.CustomItem;

import com.minecraftcivilizations.specialization.Listener.Player.Combat.CombatManager;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.Specialization;
import com.minecraftcivilizations.specialization.StaffTools.Debug;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import com.minecraftcivilizations.specialization.util.ItemStackUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.util.ARGBLike;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

import static com.minecraftcivilizations.specialization.util.MathUtils.*;
import static net.md_5.bungee.api.ChatColor.*;

public class CustomWeapon extends CustomItem{

    public CustomWeapon(String id) {
        super(id);
    }

    @Override
    public void init() {
// remove existing sword recipes
//        for (Iterator<Recipe> it = Bukkit.recipeIterator(); it.hasNext(); ) {
//            Recipe r = it.next();
//            if (r instanceof ShapedRecipe sr) {
//                ItemStack result = sr.getResult();
//                if (result.getType().name().contains("_SWORD")) {
//                    if(result.getType().equals(Material.NETHERITE_SWORD))continue;
//                    Bukkit.removeRecipe(sr.getKey());
//                }
//            }
//        }
//
//        // re-register sword recipes with custom items
//        Material[] swords = {
//                Material.WOODEN_SWORD, Material.STONE_SWORD,
//                Material.IRON_SWORD, Material.DIAMOND_SWORD,
//                Material.GOLDEN_SWORD
//        };
//
//        for (Material sword : swords) {
//            // assume createCustomItem(sword) returns an ItemStack
//            ItemStack custom = createItemStack(sword);
//
//            ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(Specialization.getInstance(), sword.name().toLowerCase()), custom);
//            recipe.shape(" I ", " I ", " S ");
//            RecipeChoice ingot = null;
//            switch(sword){
//                case WOODEN_SWORD:
//                    ingot = new RecipeChoice.MaterialChoice(
//                            Material.OAK_PLANKS,
//                            Material.ACACIA_PLANKS,
//                            Material.BIRCH_PLANKS,
//                            Material.SPRUCE_PLANKS,
//                            Material.JUNGLE_PLANKS,
//                            Material.DARK_OAK_PLANKS,
//                            Material.MANGROVE_PLANKS,
//                            Material.PALE_OAK_PLANKS,
//                            Material.BAMBOO_PLANKS
//                    );
//                    break;
//                case STONE_SWORD:
//                    ingot = new RecipeChoice.MaterialChoice(
//                            Material.STONE,
//                            Material.COBBLESTONE,
//                            Material.DEEPSLATE,
//                            Material.COBBLED_DEEPSLATE
//                    );
//                    break;
//                case IRON_SWORD:
//                    ingot = new RecipeChoice.MaterialChoice(Material.IRON_INGOT);
//                    break;
//                case GOLDEN_SWORD:
//                    ingot = new RecipeChoice.MaterialChoice(Material.GOLD_INGOT);
//                    break;
//                case DIAMOND_SWORD:
//                    ingot = new RecipeChoice.MaterialChoice(Material.DIAMOND);
//                    break;
//            }
//            if(ingot!=null) {
//                recipe.setIngredient('I', ingot);
//                recipe.setIngredient('S', Material.STICK);
//
//                Bukkit.addRecipe(recipe);
//            }
//        }
    }

    @Override
    public void onCreateItem(ItemStack itemStack, ItemMeta meta, Player player) {
        if(player!=null){
            int lvl = CoreUtil.getPlayer(player.getUniqueId()).getSkillLevel(SkillType.BLACKSMITH);
            double craft_crit_chance = 0.0;

            boolean luck_enabled = player.hasPotionEffect(PotionEffectType.LUCK);

            switch(lvl){
                default:
                    craft_crit_chance = luck_enabled?0.05:0.025;
                    break;
                case 3:
                    craft_crit_chance = luck_enabled?0.75:0.05;
                    break;
                case 4:
                    craft_crit_chance = luck_enabled?0.125:0.75;
                    break;
                case 5:
//                    craft_crit_chance = luck_enabled?0.175:0.125;
                    craft_crit_chance = luck_enabled?0.75:0.5;
                    break;
            }
            if(craft_crit_chance > rollDouble()){

                double crit_bonus = quantize((rollDouble()* 0.25 *((double)lvl)) + 0.25, 0.25);
                double crit_max = 1.5;
                Sound craft_sound = Sound.BLOCK_SMITHING_TABLE_USE;
                switch(itemStack.getType()){
                    case WOODEN_SWORD:
                        craft_sound = Sound.BLOCK_BAMBOO_WOOD_HANGING_SIGN_PLACE;
                        break;
                    case STONE_SWORD:
                        craft_sound = Sound.UI_STONECUTTER_TAKE_RESULT;
                        break;
                    case GOLDEN_SWORD:
                        crit_bonus *= 2;
                        crit_max = 3;
                        break;
                }


                if(crit_bonus >0.0){
                    ChatColor c = BLUE;
                    if(crit_bonus>=crit_max){
                        crit_bonus = Math.min(crit_max, crit_bonus);
                        c = GOLD;
                        Component original = itemStack.effectiveName().asComponent(); // or itemStack.asComponent()
                        Component recolored = original
                                .color(NamedTextColor.GOLD)
                                .decoration(TextDecoration.ITALIC, false);
                        meta.displayName(recolored);
                        itemStack.setItemMeta(meta);
                        player.playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.5f, 1.2f);
                        player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 0.125f, 1.25f);
                    }else if (crit_bonus>=1.0){
                        player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 0.125f, 1.5f);
                        c = AQUA;
                    }
                    ItemStackUtils.setLoreLine(meta, 0, c+"+"+crit_bonus+" Crit Bonus");
                    meta.getPersistentDataContainer().set(CombatManager.CRIT_BONUS_KEY, PersistentDataType.DOUBLE, crit_bonus);
                    player.playSound(player, craft_sound, 0.25f, 1.0f);
                }
            }
//            if(luck_enabled){
//                PotionEffect luck = player.getPotionEffect(PotionEffectType.LUCK);
//                PotionEffect updated = new PotionEffect(luck.getType(), Math.max(1, luck.getDuration() - ( 20 * luck_seconds_to_remove)), luck.getAmplifier(), luck.isAmbient(), luck.hasParticles(), luck.hasIcon());
////                        entity.addPotionEffect(updated);
//                player.removePotionEffect(PotionEffectType.LUCK);
//                player.addPotionEffect(luck);
//                Debug.broadcast("customitem", "luck removed");
//            }
        }
    }

    @Override
    public void onItemSwitchTo(PlayerItemHeldEvent event, ItemStack oldItem, ItemStack newItem) {
        Player player = event.getPlayer();
        int lvl = CoreUtil.getPlayer(player).getSkillLevel(SkillType.GUARDSMAN);
        if(isOnCooldown(player))return;

        boolean metal = false;
        boolean scrap = false;
        float volume = 0.5f;
        int lvl_req = 0;

        Sound sound = null;
        switch(newItem.getType()){
            case WOODEN_SWORD:
                sound = Sound.ITEM_AXE_STRIP;
                volume = 0.25f;
                break;
            case STONE_SWORD:
                sound = Sound.BLOCK_DRIPSTONE_BLOCK_BREAK;
                volume = 0.6f;
                scrap = true;
                break;
            case GOLDEN_SWORD:
                sound = Sound.ITEM_ARMOR_EQUIP_GOLD;
                volume = 0.25f;
                metal = true;
                break;
            case IRON_SWORD:
                sound = Sound.ITEM_ARMOR_EQUIP_IRON;
                metal = true;
                break;
            case DIAMOND_SWORD:
                sound = Sound.ITEM_ARMOR_EQUIP_DIAMOND;
                metal = true;
                break;
            case NETHERITE_SWORD:
                sound = Sound.ITEM_ARMOR_EQUIP_NETHERITE;
                metal = true;
                scrap = true;
                volume = 0.75f;
                break;
        }

        if(sound!=null){
            player.getWorld().playSound(player.getLocation(), sound, SoundCategory.PLAYERS,volume, random(0.9f,1.05f));
                if (scrap) {
                    player.getWorld().playSound(player.getLocation(), Sound.ITEM_AXE_SCRAPE, SoundCategory.PLAYERS, volume*1.2f, random(1.2f, 1.35f));
                }
//            if(lvl>=lvl_req) {
                if (metal) {
                    player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, volume*0.35f, random(1.19f, 1.2f));
                }
//            }
            applyCooldown(player, 10);
        }
    }

    @Override
    public void onItemSwitchAway(PlayerItemHeldEvent event, ItemStack oldItem, ItemStack newItem) {
        CustomItem ci = CustomItemManager.getInstance().getCustomItem(newItem);
//        Debug.broadcast("customitem", "old item: "+ci.getId());
        if (ci != this || oldItem.getType()!=newItem.getType()) {
            Player player = event.getPlayer();
            if(isOnCooldown(player))return;
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_HORSE_ARMOR_UNEQUIP, SoundCategory.PLAYERS, 0.25f, random(1.4f, 1.5f));
        }
    }
}
