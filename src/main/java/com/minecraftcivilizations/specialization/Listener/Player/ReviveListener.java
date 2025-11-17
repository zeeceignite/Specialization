package com.minecraftcivilizations.specialization.Listener.Player;
import com.minecraftcivilizations.specialization.Specialization;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class ReviveListener implements Listener {

    private final Map<UUID, Player> healerToDownedPlayer = new HashMap<>();

    private static final List<String> COMMON_INJURIES = List.of(
            "Severe Bleeding",
            "Broken Arm",
            "Broken Leg",
            "Concussion",
            "Fractured Rib",
            "Deep Cut",
            "Dislocated Shoulder",
            "Sprained Ankle"
    );

    public void startRevive(Player healer, Player downed, Inventory inv) {
        healer.openInventory(inv);
        healerToDownedPlayer.put(healer.getUniqueId(), downed);
    }

    public Inventory createReviveInventory(Player downed) {
        Inventory inv = Bukkit.createInventory(null, 9, Component.text("Reviving " + downed.getName()));

        // Shuffle injuries
        List<String> injuries = new ArrayList<>(COMMON_INJURIES);
        Collections.shuffle(injuries);

        Random rand = new Random();
        for (String injury : injuries) {
            // Find a random empty slot
            int slot;
            do {
                slot = rand.nextInt(inv.getSize());
            } while (inv.getItem(slot) != null);

            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(injury, NamedTextColor.RED));
            item.setItemMeta(meta);
            inv.setItem(slot, item);
        }

        return inv;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player healer)) return;
        if (!e.getView().title().toString().contains("Reviving")) return;
        e.getView().getPlayer().sendMessage("reviving");

        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null) return;

        e.getInventory().setItem(e.getSlot(), null);

        // Check if all items are gone
        boolean done = Arrays.stream(e.getInventory().getContents())
                .allMatch(Objects::isNull);

        if (done) {
            Player downed = healerToDownedPlayer.remove(healer.getUniqueId());
            if (downed == null) return;

            // Set PDC to indicate the player is no longer downed
            downed.getPersistentDataContainer().set(
                    new NamespacedKey(Specialization.getInstance(), "is_downed"),
                    PersistentDataType.BYTE,
                    (byte) 0
            );

            healer.closeInventory();
        }
    }
}
