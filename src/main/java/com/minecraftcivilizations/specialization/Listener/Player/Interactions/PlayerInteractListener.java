package com.minecraftcivilizations.specialization.Listener.Player.Interactions;

import com.google.gson.reflect.TypeToken;
import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.Skill;
import com.minecraftcivilizations.specialization.Skill.SkillLevel;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;
import org.bukkit.ChatColor;
import java.util.regex.Pattern;

import java.util.*;

public class PlayerInteractListener implements Listener {

    @EventHandler
    public void onOpenBlockInventory(InventoryOpenEvent e) {
        if(e.getPlayer().isOp()) return;

        InventoryType type = e.getInventory().getType();
        List<InventoryType> defaultAllow = SpecializationConfig.getCanUseBlockConfig().get("default", new TypeToken<>(){});
        if(defaultAllow.contains(type)) return;

        CustomPlayer player = CoreUtil.getPlayer(e.getPlayer());
        for (Skill skill : player.getSkills()) {
            // Check all skill levels from NOVICE up to the player's current level
            SkillType skillType = skill.getSkillType();
            int playerSkillLevel = player.getSkillLevel(skillType);

            for (SkillLevel skillLevel : SkillLevel.values()) {
                if (skillLevel.getLevel() <= playerSkillLevel) {
                    String configKey = skillType + "_" + skillLevel;
                    List<InventoryType> types = SpecializationConfig.getCanUseBlockConfig().get(configKey, new TypeToken<>(){});
                    if (types != null && types.contains(type)) {
                        return; // Player has access through this skill level
                    }
                }
            }
        }
        e.setCancelled(true);
    }

    @EventHandler
    public void onWaterSmushCrop(BlockFromToEvent e){
        if(e.getToBlock().getBlockData() instanceof Ageable){
            e.getToBlock().setType(Material.AIR);
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDestoryFarmland(BlockBreakEvent e){
        if(e.getBlock().getType().equals(Material.FARMLAND)){
            e.getBlock().getRelative(BlockFace.UP).setType(Material.AIR);
            e.getBlock().setType(Material.AIR);
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDestroySugarcaneSource(BlockBreakEvent e){
        Block nextBlock = e.getBlock().getRelative(BlockFace.UP);
        if(e.getBlock().getType().equals(Material.SUGAR_CANE)) return;
        while(nextBlock.getType().equals(Material.SUGAR_CANE)){
            nextBlock.setType(Material.AIR);
            nextBlock = nextBlock.getRelative(BlockFace.UP);
        }
    }

    @EventHandler
    public void onPlayerSmushCrop(PlayerInteractEvent e){
        if(e.getAction().equals(Action.PHYSICAL)){
            if(e.getClickedBlock() == null){
                return;
            }

            if(e.getClickedBlock().getType().equals(Material.FARMLAND)){
                e.setCancelled(true);
                e.getClickedBlock().setType(Material.DIRT);
                e.getClickedBlock().getRelative(BlockFace.UP).setType(Material.AIR);
            }
        }
    }

    @EventHandler
    public void onLibrarianEnchantItem(PlayerInteractEvent e) {
        if (!e.getAction().isRightClick() || !e.getPlayer().isSneaking() || e.getItem() == null || e.getHand().equals(EquipmentSlot.OFF_HAND))
            return;
        if (!e.getPlayer().getInventory().getItemInOffHand().getType().equals(Material.BOOK))
            return;

        CustomPlayer player = CoreUtil.getPlayer(e.getPlayer());
        int xpBase = SpecializationConfig.getLibrarianConfig().get("BLESS_ITEM_XP_LEVEL_REQUIREMENT", Integer.class);
        int skillMin = SpecializationConfig.getLibrarianConfig().get("BLESS_ITEM_LIBRARIAN_LEVEL", Integer.class);
        int xpLevelAmount = xpBase * (player.getSkillLevel(SkillType.LIBRARIAN) - skillMin + 1);
        if (xpLevelAmount > e.getPlayer().getLevel()) return;

        String regex = SpecializationConfig.getLibrarianConfig().get("ENCHANTABLE_TOOL_REGEX", String.class);
        String typeName = e.getItem().getType().name().toLowerCase();
        if (!Pattern.compile(regex).matcher(typeName).find()) return;
        if (player.getSkillLevel(SkillType.LIBRARIAN) < skillMin) return;

        ItemMeta meta = e.getItem().getItemMeta();
        if (meta == null) return;

        // prevent re-blessing
        if (meta.hasLore()) {
            for (Component c : meta.lore()) {
                if (((net.kyori.adventure.text.TextComponent) c).content().toLowerCase().contains("blessed")) {
                    e.getPlayer().sendMessage(ChatColor.RED + "This item has already been blessed.");
                    return;
                }
            }
        }

        List<NamespacedKey> bannedBlessEnchants =
                SpecializationConfig.getLibrarianConfig().get("BANNED_BLESS_ENCHANTS", new TypeToken<>() {});

        List<Enchantment> validEnchants = RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.ENCHANTMENT)
                .stream()
                .filter(enchant -> {
                    if (bannedBlessEnchants.contains(enchant.getKey())) return false;
                    if (!enchant.canEnchantItem(e.getItem())) return false;
                    for (Enchantment existing : e.getItem().getEnchantments().keySet()) {
                        if (enchant.conflictsWith(existing)) return false;
                    }
                    return true;
                })
                .toList();

        if (validEnchants.isEmpty()) {
            e.getPlayer().sendMessage(ChatColor.RED + "This item cannot be blessed further.");
            return;
        }

        Enchantment enchant = validEnchants.get(new Random().nextInt(validEnchants.size()));
        int level = new Random().nextInt(1 + player.getSkillLevel(SkillType.LIBRARIAN) - skillMin);
        if (level <= 0) level = 1;
        int finalLevel = Math.min(enchant.getMaxLevel(), level);

        meta.addEnchant(enchant, finalLevel, false);

        List<Component> lore = meta.hasLore() ? new ArrayList<>(Objects.requireNonNull(meta.lore())) : new ArrayList<>();
        String enchantDisplay = enchant.getKey().getKey().replace("_", " ");
        lore.add(Component.text(ChatColor.GOLD + "Blessed with " + enchantDisplay + " " + finalLevel + " by " + e.getPlayer().getName()));
        meta.lore(lore);
        e.getItem().setItemMeta(meta);

        e.getPlayer().setLevel(e.getPlayer().getLevel() - xpLevelAmount);
        e.getPlayer().getInventory().getItemInOffHand()
                .setAmount(e.getPlayer().getInventory().getItemInOffHand().getAmount() - 1);

        e.getPlayer().sendMessage(ChatColor.GOLD + "✨ Your " + typeName.replace("_", " ") + " has been blessed with " + enchantDisplay + " " + finalLevel + "!");
    }

    @EventHandler
    public void onHarvestSweetBerries(PlayerInteractEvent e) {
        // main-hand right click on a fully-grown sweet-berry bush
        if (!e.getAction().isRightClick() || e.getHand() == EquipmentSlot.OFF_HAND) return;
        Block clicked = e.getClickedBlock();
        if (clicked == null || clicked.getType() != Material.SWEET_BERRY_BUSH) return;

        org.bukkit.block.data.BlockData data = clicked.getBlockData();
        if (data instanceof Ageable age && age.getAge() == age.getMaximumAge()) {
            CustomPlayer cp = CoreUtil.getPlayer(e.getPlayer());
            if (cp != null) {
                // tweak value if you like; 1 is a safe default
                cp.addSkillXp(SkillType.FARMER, 3);
            }
        }
    }
    @EventHandler
    public void onHarvestGlowBerries(PlayerInteractEvent e) {
        if (!e.getAction().isRightClick() || e.getHand() == EquipmentSlot.OFF_HAND) return;
        Block clicked = e.getClickedBlock();
        if (clicked == null) return;

        Material type = clicked.getType();
        if (type != Material.CAVE_VINES && type != Material.CAVE_VINES_PLANT) return;

        String data = clicked.getBlockData().getAsString();
        if (!data.contains("berries=true")) return; // only when berries are actually present

        CustomPlayer cp = CoreUtil.getPlayer(e.getPlayer());
        if (cp != null) {
            cp.addSkillXp(SkillType.FARMER, 1); // adjust XP if you like
        }
    }

    @EventHandler
    public void onMilk(PlayerItemConsumeEvent e) {
        if (e.getItem().getType() != Material.MILK_BUCKET) return;

        // milk clears potion effects; re-apply class passives next tick
        Bukkit.getScheduler().runTaskLater(
                com.minecraftcivilizations.specialization.Specialization.getInstance(),
                () -> {
                    CustomPlayer cp = CoreUtil.getPlayer(e.getPlayer());
                    if (cp != null) {
                        cp.applyEffects(); // your existing method that reapplies class bonus effects
                    }
                },
                1L
        );
    }

    @EventHandler
    public void onAnvilFinish(InventoryClickEvent e) {
        if(e.getView() instanceof AnvilView view){
            String renameText = view.getRenameText();
            if(renameText != null && renameText.matches("^\\[lore [0-9]].*")){
                CustomPlayer player = CoreUtil.getPlayer(e.getWhoClicked());
                int level = SpecializationConfig.getLibrarianConfig().get("ITEM_LORE_LIBRARIAN_LEVEL", Integer.class);
                if(player.getSkillLevel(SkillType.LIBRARIAN) < level) return;

                int number = Integer.parseInt(String.valueOf(renameText.charAt(6)));
                ItemStack result = view.getTopInventory().getResult();
                if(result == null) return;
                ItemStack oldItem = view.getTopInventory().getFirstItem();
                if (oldItem.hasData(DataComponentTypes.CUSTOM_NAME))
                    result.setData(DataComponentTypes.CUSTOM_NAME, view.getTopInventory().getFirstItem().getData(DataComponentTypes.CUSTOM_NAME));
                else {
                    result.unsetData(DataComponentTypes.CUSTOM_NAME);
                }
                ArrayList<Component> lines = new ArrayList<>(result.getData(DataComponentTypes.LORE).lines());
                if(lines.size() < number) {
                    for (int i = 0; i < number - lines.size() + 1; i++) lines.add(Component.empty());
                }
                lines.set(number - 1, Component.text(renameText.substring(8).trim()));

                result.setData(DataComponentTypes.LORE, ItemLore.lore(lines));
            }
        }
    }


    @EventHandler
    public void anvilRenameEvent(InventoryClickEvent e) {
        if(e.getView() instanceof AnvilView view){
            String renameText = view.getRenameText();
            if(renameText != null && renameText.matches("^\\[lore [0-9]]")){
                int number = renameText.charAt(7);
                ItemStack result = view.getTopInventory().getResult();
                result.unsetData(DataComponentTypes.CUSTOM_NAME);
                result.getData(DataComponentTypes.LORE).lines().add(number + 1, Component.text(renameText.substring(8)));
            }
        }
    }

    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent e ) {
        if(e.getBucket().equals(Material.LAVA_BUCKET)){
            CustomPlayer player = CoreUtil.getPlayer(e);
            if(player.getSkillLevel(SkillType.BLACKSMITH) < SkillLevel.EXPERT.getLevel()) e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBucketFill(PlayerBucketFillEvent e ) {
        if(e.getBucket().equals(Material.LAVA_BUCKET)){
            CustomPlayer player = CoreUtil.getPlayer(e);
            if(player.getSkillLevel(SkillType.BLACKSMITH) < SkillLevel.EXPERT.getLevel()) e.setCancelled(true);
        }
    }



}