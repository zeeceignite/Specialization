package com.minecraftcivilizations.specialization.GUI;

import minecraftcivilizations.com.minecraftCivilizationsCore.GUI.GUI;
import minecraftcivilizations.com.minecraftCivilizationsCore.GUI.GUIItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public class PatDownGUI extends GUI {

    private final Player target;
    private final Player inspector;

    public PatDownGUI(Player inspector, Player target) {
        super(Component.text("Pat Down: " + target.getName()).color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false), 54);
        this.target = target;
        this.inspector = inspector;
    }

    @Override
    public void open(Player player) {
        this.getItems().clear();

        PlayerInventory targetInv = target.getInventory();


        for (int i = 0; i < 9; i++) {
            ItemStack item = targetInv.getItem(i);
            if (item != null) {
                this.getItems().put(i, new GUIItem(item.clone(), null));
            }
        }

        for (int i = 9; i < 36; i++) {
            ItemStack item = targetInv.getItem(i);
            if (item != null) {
                this.getItems().put(i, new GUIItem(item.clone(), null));
            }
        }


        ItemStack helmet = targetInv.getHelmet();
        if (helmet != null) {
            this.getItems().put(36, new GUIItem(helmet.clone(), null));
        } else {
            this.getItems().put(36, new GUIItem(createPlaceholder("Helmet Slot"), null));
        }

        ItemStack chestplate = targetInv.getChestplate();
        if (chestplate != null) {
            this.getItems().put(37, new GUIItem(chestplate.clone(), null));
        } else {
            this.getItems().put(37, new GUIItem(createPlaceholder("Chestplate Slot"), null));
        }

        ItemStack leggings = targetInv.getLeggings();
        if (leggings != null) {
            this.getItems().put(38, new GUIItem(leggings.clone(), null));
        } else {
            this.getItems().put(38, new GUIItem(createPlaceholder("Leggings Slot"), null));
        }

        ItemStack boots = targetInv.getBoots();
        if (boots != null) {
            this.getItems().put(39, new GUIItem(boots.clone(), null));
        } else {
            this.getItems().put(39, new GUIItem(createPlaceholder("Boots Slot"), null));
        }

        // Offhand
        ItemStack offhand = targetInv.getItemInOffHand();
        this.getItems().put(40, new GUIItem(offhand.clone(), null));

        ItemStack barrier = createPlaceholder("Empty");
        for (int i = 41; i < 54; i++) {
            this.getItems().put(i, new GUIItem(barrier, null));
        }

        // Send messages
        inspector.sendMessage("§6[Pat Down] §eInspecting " + target.getName() + "'s belongings...");
        target.sendMessage("§c[Pat Down] §e" + inspector.getName() + " is inspecting your belongings.");

        super.open(player);
    }

    private ItemStack createPlaceholder(String name) {
        ItemStack placeholder = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = placeholder.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(ItemFlag.values());
            meta.displayName(Component.text(name).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(Component.text("This slot is empty").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)));
            placeholder.setItemMeta(meta);
        }
        return placeholder;
    }
}
