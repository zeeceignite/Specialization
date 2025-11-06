package com.minecraftcivilizations.specialization.CustomItem;

import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.Specialization;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * @author jfrogy
 */
public class Bandage extends CustomItem {

    public Bandage(String id, String displayName) {
        super(id, displayName, org.bukkit.Material.PAPER);
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
    public void onCreateItem(ItemStack itemStack, ItemMeta meta) {
        meta.setEnchantmentGlintOverride(true);
        meta.lore(java.util.List.of(
                Component.text("Shift + Right Click to heal yourself.").color(NamedTextColor.BLUE),
                Component.text("Right Click a player/passive mob to heal them.").color(NamedTextColor.BLUE),
                Component.empty(),
                Component.text("Amount Healed and XP gained scale with Healer level.").color(NamedTextColor.GRAY)
        ));
        itemStack.setItemMeta(meta);
    }

    // Handles entity interaction (healing target)
    @Override
    public void onInteractEntity(PlayerInteractEntityEvent event, ItemStack itemStack) {
        Player healer = event.getPlayer();


        LivingEntity target = (LivingEntity) event.getRightClicked();
        if (target.getHealth() >= target.getAttribute(Attribute.MAX_HEALTH).getValue()) return; // only heal if not full health

        healer.setCooldown(itemStack.getType(), (int) 200);
        applyHeal(healer, target, itemStack);
    }

    // Handles self-heal if sneak + right click air/block
    @Override
    public void onInteract(PlayerInteractEvent event, ItemStack itemStack) {
        Player healer = event.getPlayer();

        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR && event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;


        if (!healer.isSneaking()) return; // only handle sneak self-heal here

        healer.setCooldown(itemStack.getType(), (int) 500);
        applyHeal(healer, healer, itemStack);
    }

    private void applyHeal(Player healer, LivingEntity target, ItemStack bandage) {

        CustomPlayer cHealer = CoreUtil.getPlayer(healer.getUniqueId());
        int lvl = cHealer.getSkillLevel(SkillType.HEALER);
        if (lvl == 0) return;

        if (healer.getFoodLevel() < 3) {
            healer.sendMessage("§cYou need at least 3 hunger to use a Bandage.");
            return;
        }


        int level = Math.min(lvl, 5);
        double healAmount = 2 + ((level - 1) * (8.0 / 4.0));
        int xp = 15 + (int) ((level - 1) * (35.0 / 4.0));

        double newHealth = Math.min(target.getHealth() + healAmount, target.getAttribute(Attribute.MAX_HEALTH).getValue());
        target.setHealth(newHealth);
        healer.setFoodLevel(healer.getFoodLevel() - 3);
        if (!(target instanceof Enemy)) {
            cHealer.addSkillXp(SkillType.HEALER, xp);
        }
        if (target instanceof Player pTarget) {
            CoreUtil.getPlayer(pTarget.getUniqueId()).setDowned(false);
        }

        // Heart particles
        int particleCount = (int) Math.ceil(healAmount / 2.0);
        World w = target.getWorld();
        w.spawnParticle(Particle.HEART, target.getLocation().add(0, 0.75, 0),
                particleCount, 0.3, 0.3, 0.3, 0.05);
        w.playSound(target.getEyeLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1, 1);

        // Consume one Bandage
        bandage.setAmount(bandage.getAmount() - 1);

        // Build hearts message
        double maxHealth = target.getAttribute(Attribute.MAX_HEALTH).getValue();
        int totalHearts = (int) Math.ceil(maxHealth / 2.0);
        int redHearts = (int) Math.ceil(newHealth / 2.0);
        int grayHearts = totalHearts - redHearts;

        String heartsMsg = "<red>❤</red>".repeat(Math.max(0, redHearts)) +
                "<gray>❤</gray>".repeat(Math.max(0, grayHearts));
        //debug
        healer.sendMessage(MiniMessage.miniMessage().deserialize(
                "<green>Used " + getDisplayName() + " on " + target.getName() + " for " + healAmount + " HP. " +
                        heartsMsg
        ));
    }
}
