package com.minecraftcivilizations.specialization.Listener;

import com.google.gson.reflect.TypeToken;
import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.Skill;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;

import java.util.*;

public class PlayerInteractListener implements Listener {

    @EventHandler
    public void onRightClickBlock(InventoryOpenEvent e) {
        if(e.getPlayer().isOp()) return;
        InventoryType type = e.getInventory().getType();
        List<InventoryType> defaultAllow = SpecializationConfig.getCanUseBlockConfig().get("default", new TypeToken<>(){});
        if(defaultAllow.contains(type)) return;

        CustomPlayer player = CoreUtil.getPlayer(e.getPlayer());
        for (Skill skill : player.getSkills()) {
            List<InventoryType> types = SpecializationConfig.getCanUseBlockConfig().get(skill.getSkillType()+"_"+player.getSkillLevelEnum(skill.getSkillType()), new TypeToken<>(){});
            if(types.contains(type)) return;
        }
        e.setCancelled(true);
    }

    @EventHandler
    public void onLibrarianEnchantItem(PlayerInteractEvent e){
        if(!e.getAction().isRightClick() || !e.getPlayer().isSneaking() || e.getItem() == null) return;
        CustomPlayer player = CoreUtil.getPlayer(e.getPlayer());

        int xpBase = SpecializationConfig.getLibrarianConfig().get("BLESS_ITEM_XP_LEVEL_REQUIREMENT", Integer.class);
        int skillMin = SpecializationConfig.getLibrarianConfig().get("BLESS_ITEM_LIBRARIAN_LEVEL", Integer.class);
        int xpLevelAmount = xpBase * (player.getSkillLevel(SkillType.LIBRARIAN) - skillMin + 1);
        if(xpLevelAmount > e.getPlayer().getLevel()) return;

        String regex = SpecializationConfig.getLibrarianConfig().get("ENCHANTABLE_TOOL_REGEX", String.class);
        if(e.getItem().getType().name().matches(regex) && player.getSkillLevel(SkillType.LIBRARIAN) >= skillMin) {
            List<NamespacedKey> bannedBlessEnchants = SpecializationConfig.getLibrarianConfig().get("BANNED_BLESS_ENCHANTS", new TypeToken<>(){});

            ArrayList<Enchantment> validEnchants = new ArrayList<>(RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)
                    .stream().filter(enchant -> {
                        boolean conflict = e.getItem().getEnchantments().keySet().stream().anyMatch(enchant::conflictsWith);
                        return enchant.canEnchantItem(e.getItem()) && !conflict && !bannedBlessEnchants.contains(enchant.getKey());
                    }).toList());

            if(validEnchants.isEmpty()) return;
            Collections.shuffle(validEnchants);
            ItemMeta meta = e.getItem().getItemMeta();
            Enchantment enchant = validEnchants.getFirst();

            int level = new Random().nextInt(1 + player.getSkillLevel(SkillType.LIBRARIAN) - skillMin);
            meta.addEnchant(enchant, Math.min(enchant.getMaxLevel(), level), false);

            ArrayList<Component> lore = meta.hasLore() ? new ArrayList<>(Objects.requireNonNull(meta.lore())) :  new ArrayList<>();
            lore.add(MiniMessage.miniMessage().deserialize("<blue>This item was blessed with ").append(enchant.displayName(1), Component.text(" by "), player.getName()));
            meta.lore(lore);

            e.getItem().setItemMeta(meta);
            e.getPlayer().setLevel(e.getPlayer().getLevel() - xpLevelAmount);
            List<ItemStack> items = Arrays.stream(e.getPlayer().getInventory().getContents()).filter(Objects::nonNull).toList();
            int index = new Random().nextInt(items.size());
            items.get(index).setAmount(items.get(index).getAmount() - 1);
        }
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



}
