package com.minecraftcivilizations.specialization.CustomItem;

import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.Specialization;
import com.minecraftcivilizations.specialization.StaffTools.Debug;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.UseCooldownComponent;
import org.bukkit.persistence.PersistentDataType;

import static net.md_5.bungee.api.ChatColor.*;

/**
 * @author jfrogy, alectriciti
 */
public class Bandage extends CustomItem {

    public Bandage(String id, String displayName) {
        super(id, displayName, org.bukkit.Material.PAPER, true);
    }

    NamespacedKey RECIPE_KEY = new NamespacedKey(Specialization.getInstance(), "bandage_recipe");
    /**
     * Called when loading/reloading
     */
    public void init(){
//        Bukkit.getRecipe()
        if(Bukkit.getRecipe(RECIPE_KEY)!=null) {
            Bukkit.removeRecipe(RECIPE_KEY);
        }

        ShapelessRecipe bandage_recipe = new ShapelessRecipe(RECIPE_KEY, createItemStack(1));
        bandage_recipe.addIngredient(8, Material.PAPER);
        bandage_recipe.addIngredient(Material.SUGAR_CANE);

        Bukkit.addRecipe(bandage_recipe, true);
    }


    @Override
    public void onCreateItem(ItemStack itemStack, ItemMeta meta, Player player_who_crafted) {
        meta.setEnchantmentGlintOverride(true);
        meta.lore(java.util.List.of(
                Component.text("Shift + Right Click to heal yourself.").color(NamedTextColor.BLUE),
                Component.text("Right Click a player/passive mob to heal them.").color(NamedTextColor.BLUE),
                Component.empty(),
                Component.text("Amount Healed and XP gained scale with Healer level.").color(NamedTextColor.GRAY),
                Component.text("Crafted by "+(player_who_crafted!=null?player_who_crafted.getName():"nobody")).color(NamedTextColor.GRAY)
        ));
        itemStack.setItemMeta(meta);
    }

    @Override
    public boolean canPlayerCraft(Player player) {
        return CoreUtil.getPlayer(player.getUniqueId()).getSkillLevel(SkillType.HEALER) > 0;
    }

    // Handles entity interaction (healing target)

    @Override
    public void onInteractEntity(PlayerInteractEntityEvent event, ItemStack itemStack) {
        if (itemStack == null) return;

        Player healer = event.getPlayer();
        Entity clicked = event.getRightClicked();

        // Only heal non-hostile living entities
        if (clicked instanceof Monster) return;

        if (!(clicked instanceof LivingEntity target)) return;

        // Only heal if not full health
        AttributeInstance maxHealth = target.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null || target.getHealth() >= maxHealth.getValue()) return;

        if (isOnCooldown(healer)) return;

        applyHeal(healer, target, itemStack);
    }

    // Handles self-heal if sneak + right click air/block
    @Override
    public void onInteract(PlayerInteractEvent event, ItemStack itemStack) {
        if (itemStack == null) return;

        Player healer = event.getPlayer();

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        if (!healer.isSneaking()) return; // only handle sneak self-heal here

        if (isOnCooldown(healer))return;

        applyHeal(healer, healer, itemStack);
    }


    private void applyHeal(Player healer, LivingEntity target, ItemStack bandage) {


        Debug.broadcast("customitem", "<green>applying heal");
        CustomPlayer cHealer = CoreUtil.getPlayer(healer.getUniqueId());
        int lvl = cHealer.getSkillLevel(SkillType.HEALER);
        if (lvl == 0) return;

        double current_health = target.getHealth();
        double max_health = target.getAttribute(Attribute.MAX_HEALTH).getValue();
        if(current_health >= max_health){
            Debug.broadcast("customitem", "<red>returned in maxheal");
            return;
        }

        if (healer.getFoodLevel() < 3) {
            healer.sendMessage("§cYou're too hungry to preform this action");
            return;
        }

        int level = Math.min(lvl, 5);
        double heal_amount = 2 + ((level - 1) * (8.0 / 4.0));
        int xp = 15 + (int) ((level - 1) * (35.0 / 4.0));
        double new_health = Math.min(current_health + heal_amount, max_health);
        target.setHealth(new_health);
        healer.setFoodLevel(healer.getFoodLevel() - 3);

        if(target.equals(healer)){
            applyCooldown(healer, 1200);
        }else{
            applyCooldown(healer, 150);
        }
        if (!(target instanceof Enemy)) {
            cHealer.addSkillXp(SkillType.HEALER, xp);
        }
        if (target instanceof Player pTarget) {
            CoreUtil.getPlayer(pTarget.getUniqueId()).setDowned(false);
        }

        // Heart particles
        int particleCount = (int) Math.ceil(heal_amount / 2.0);
        World w = target.getWorld();
        w.spawnParticle(Particle.HEART, target.getLocation().add(0, 0.75, 0),
                particleCount, 0.3, 0.3, 0.3, 0.05);
        w.playSound(target.getEyeLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1, 1);
        w.playSound(target.getEyeLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 0.85f, 1.2f);

        // Consume one Bandage
        bandage.setAmount(bandage.getAmount() - 1);

        // Build hearts message
        double maxHealth = target.getAttribute(Attribute.MAX_HEALTH).getValue();
        int totalHearts = (int) Math.ceil(maxHealth / 2.0);
        int redHearts = (int) Math.ceil(new_health / 2.0);
        int grayHearts = totalHearts - redHearts;

        String heartsMsg = "<red>❤</red>".repeat(Math.max(0, redHearts)) +
                "<gray>❤</gray>".repeat(Math.max(0, grayHearts));
        healer.sendMessage(MiniMessage.miniMessage().deserialize(heartsMsg));
        //debug msg
        Debug.broadcast("customitem_"+healer.getName().toLowerCase(), "message of "+healer.getName());
        Debug.message(healer,"customitem",("<green>Used " + getDisplayName() + " on " + target.getName() + " for " + heal_amount + " HP. " + heartsMsg));
    }
}
