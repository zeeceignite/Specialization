package com.minecraftcivilizations.specialization.Listener;

import com.google.gson.reflect.TypeToken;
import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.Skill;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class PlayerInteractListener implements Listener {

    @EventHandler
    public void onRightClickBlock(PlayerInteractEvent e) {
        if(e.getAction().isLeftClick() || e.getClickedBlock() == null) return;
        List<Material> defaultAllow = SpecializationConfig.getCanUseBlockConfig().get("default", new TypeToken<>(){});
        Material clickedType = e.getClickedBlock().getType();
        if(defaultAllow.contains(clickedType)) return;

        CustomPlayer player = CoreUtil.getPlayer(e.getPlayer());
        for (Skill skill : player.getSkills()) {
            List<Material> blocks = SpecializationConfig.getCanUseBlockConfig().get(skill.getSkillType()+"_"+player.getSkillLevelEnum(skill.getSkillType()), new TypeToken<>(){});
            if(blocks.contains(clickedType)) return;
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
        if(xpLevelAmount > e.getPlayer().getExpToLevel()) return;


        String regex = SpecializationConfig.getLibrarianConfig().get("ENCHANTABLE_TOOL_REGEX", String.class);
        if(regex.matches(e.getItem().getType().name()) && player.getSkillLevel(SkillType.LIBRARIAN) >= skillMin) {

            List<Enchantment> bannedBlessEnchants = SpecializationConfig.getLibrarianConfig().get("BANNED_BLESS_ENCHANTS", new TypeToken<>(){});

            ArrayList<Enchantment> validEnchants = new ArrayList<>(RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)
                    .stream().filter(enchant -> {
                        boolean conflict = e.getItem().getEnchantments().keySet().stream().anyMatch(enchant::conflictsWith);
                        return enchant.canEnchantItem(e.getItem()) && !conflict && !bannedBlessEnchants.contains(enchant);
                    }).toList());

            if(validEnchants.isEmpty()) return;
            Collections.shuffle(validEnchants);
            ItemMeta meta = e.getItem().getItemMeta();
            Enchantment enchant = validEnchants.getFirst();

            int level = new Random().nextInt(1 + player.getSkillLevel(SkillType.LIBRARIAN) - skillMin);
            meta.addEnchant(enchant, Math.min(enchant.getMaxLevel(), level), false);

            ArrayList<Component> lore = meta.hasLore() ? new ArrayList<>(Objects.requireNonNull(meta.lore())) :  new ArrayList<>();
            lore.add(MiniMessage.miniMessage().deserialize("<blue>This item was blessed with " + enchant.displayName(1) + " by ").append(player.getName()));
            meta.lore(lore);

            e.getPlayer().setLevel(e.getPlayer().getLevel() - xpLevelAmount);
            List<ItemStack> items = Arrays.stream(e.getPlayer().getInventory().getContents()).filter(Objects::nonNull).toList();
            int index = new Random().nextInt(items.size());
            items.get(index).setAmount(items.get(index).getAmount() - 1);
        }
    }


}
