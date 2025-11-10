package com.minecraftcivilizations.specialization.util;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ItemStackUtils {

    /**
     * Returns true if the item has a non-empty lore line at the given index.
     */
    public static boolean hasLoreLine(@NotNull ItemStack item_stack, int line) {
        ItemMeta meta = item_stack.getItemMeta();
        if (meta == null || !meta.hasLore()) return false;

        List<String> lore = meta.getLore();
        if (lore == null) return false;
        return line < lore.size() && lore.get(line) != null && !lore.get(line).isEmpty();
    }

    /**
     * Sets/overwrites a single lore line on the item.
     * Preserves other existing lines. Expands the lore list with empty strings if necessary.
     */
    public static void setLoreLine(@NotNull ItemStack item_stack, int line, String loreText) {
        ItemMeta meta = item_stack.getItemMeta();
        if (meta == null) return;

        List<String> lore_list = meta.hasLore()
                ? new ArrayList<>(Objects.requireNonNull(meta.getLore()))
                : new ArrayList<>();

        // Ensure list is large enough
        while (lore_list.size() <= line) {
            lore_list.add("");
        }

        lore_list.set(line, loreText == null ? "" : loreText);
        meta.setLore(lore_list);
        item_stack.setItemMeta(meta);
    }

    /**
     * Sets/overwrites a single lore line on the item.
     * Preserves other existing lines. Expands the lore list with empty strings if necessary.
     */
    public static void setLoreLine(ItemMeta meta, int line, String loreText) {
        List<String> lore_list = meta.hasLore()
                ? new ArrayList<>(Objects.requireNonNull(meta.getLore()))
                : new ArrayList<>();

        // Ensure list is large enough
        while (lore_list.size() <= line) {
            lore_list.add("");
        }

        lore_list.set(line, loreText == null ? "" : loreText);
        meta.setLore(lore_list);
    }

    /**
     * Returns true if the item has the given namespaced key flag in its ItemMeta PDC.
     */
    public static boolean hasLoreTag(ItemStack item_stack, NamespacedKey key) {
        if (item_stack == null || key == null) return false;
        if (!item_stack.hasItemMeta()) return false;

        ItemMeta meta = item_stack.getItemMeta();
        if (meta == null) return false;

        return meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    /**
     * Sets/overwrites a single lore line on the item and marks it with the given NamespacedKey in the PDC.
     * If the flag already exists this method does nothing
     */
    public static void setLoreTag(ItemStack item_stack, NamespacedKey key, int line, String lore) {
        if (item_stack == null || key == null || line < 0) return;

        ItemMeta meta = item_stack.getItemMeta();
        if (meta == null) return;

        // If already tagged, don't reapply
        if (meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE)) return;

        List<String> lore_list = meta.hasLore()
                ? new ArrayList<>(Objects.requireNonNull(meta.getLore()))
                : new ArrayList<>();

        // Expand to required size (fill with empty strings)
        while (lore_list.size() <= line) {
            lore_list.add("");
        }

        lore_list.set(line, lore == null ? "" : lore);

        meta.setLore(lore_list);
        // mark with a byte flag (value 1)
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

        item_stack.setItemMeta(meta);
    }

}
