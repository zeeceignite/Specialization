package com.minecraftcivilizations.specialization.Listener.Player;

import com.minecraftcivilizations.specialization.Specialization;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class ReviveListener implements Listener {

    private static final NamespacedKey INJURY_KEY = new NamespacedKey(Specialization.getInstance(), "revive_injury");
    private static final List<String> COMMON_INJURIES = List.of(
            "Severe Bleeding",
            "Broken Arm",
            "Broken Leg",
            "Concussion",
            "Fractured Rib",
            "Deep Cut",
            "Dislocated Shoulder",
            "Sprained Ankle",
            "Owie"
    );
    private static final List<Material> RED_MATERIALS = List.of(
            Material.SPIDER_EYE,
            Material.REDSTONE,
            Material.RED_DYE,
            Material.BEEF,
            Material.MUTTON,
            Material.NETHER_WART,
            Material.NETHER_WART_BLOCK,
            Material.BONE_MEAL
    );

    private static final List<String> HEALTHY_THINGS = List.of(
            "Healthy Heart",
            "Healthy Lungs",
            "Healthy Liver",
            "Healthy Kidneys",
            "Healthy Stomach",
            "Healthy Muscles",
            "Healthy Bones",
            "Healthy Blood"
    );

    private static final List<Material> HEALTHY_MATERIALS = List.of(
            Material.FERMENTED_SPIDER_EYE,
            Material.RABBIT_FOOT,
            Material.BEETROOT,
            Material.SWEET_BERRIES,
            Material.RED_GLAZED_TERRACOTTA,
            Material.BONE,
            Material.HONEYCOMB
    );

    private final Map<UUID, Player> healerToDownedPlayer = new HashMap<>();
    private final Map<UUID, BossBar> downedBossBars = new HashMap<>();

    public void startRevive(Player healer, Player downed, Inventory inv) {
        healer.openInventory(inv);
        healer.playSound(healer.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1.2f);
        healerToDownedPlayer.put(healer.getUniqueId(), downed);

        // Create BossBar for downed player
        BossBar bar = Bukkit.createBossBar(
                "Revive Progress", // String instead of Component
                BarColor.GREEN,
                BarStyle.SOLID
        );
        bar.addPlayer(downed);
        bar.setProgress(0.0);
        downedBossBars.put(downed.getUniqueId(), bar);
    }

    public Inventory createReviveInventory(Player downed) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Reviving " + downed.getName(), NamedTextColor.RED));
        Random rand = new Random();

        List<String> injuries = new ArrayList<>(COMMON_INJURIES);
        Collections.shuffle(injuries);

        for (String injury : injuries) {
            int slot = getRandomEmptySlot(inv, rand);
            inv.setItem(slot, createInjuryItem(injury));
        }

        for (int i = 0; i < 6; i++) {
            int slot = getRandomEmptySlot(inv, rand);
            inv.setItem(slot, createHealthyItem());
        }

        return inv;
    }

    private int getRandomEmptySlot(Inventory inv, Random rand) {
        int slot;
        do {
            slot = rand.nextInt(inv.getSize());
        } while (inv.getItem(slot) != null);
        return slot;
    }

    private ItemStack createInjuryItem(String injury) {
        Material mat = RED_MATERIALS.get(new Random().nextInt(RED_MATERIALS.size()));
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(injury, NamedTextColor.RED));
        meta.getPersistentDataContainer().set(INJURY_KEY, PersistentDataType.BYTE, (byte) 1); // mark as injury
        item.setItemMeta(meta);
        return item;
    }


    private ItemStack createHealthyItem() {
        Random rand = new Random();
        Material mat = HEALTHY_MATERIALS.get(rand.nextInt(HEALTHY_MATERIALS.size()));
        String trait = HEALTHY_THINGS.get(rand.nextInt(HEALTHY_THINGS.size()));

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(trait, NamedTextColor.GREEN));
        item.setItemMeta(meta);

        return item;
    }

    private ItemStack createBandageItem() {
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        meta.displayName(Component.text("Bandage", NamedTextColor.WHITE));
        meta.setEnchantmentGlintOverride(true);
        paper.setItemMeta(meta);
        return paper;
    }

    private void addRandomInjuries(Inventory inv, int amount) {
        Random rand = new Random();
        List<String> injuries = new ArrayList<>(COMMON_INJURIES);
        Collections.shuffle(injuries);

        for (int i = 0; i < amount; i++) {
            int slot = getRandomEmptySlot(inv, rand);
            inv.setItem(slot, createInjuryItem(injuries.get(i % injuries.size())));
        }
    }

    private void updateBossBarProgress(Player downed, Inventory inv) {
        BossBar bar = downedBossBars.get(downed.getUniqueId());
        if (bar == null) return;

        long totalInjuries = Arrays.stream(inv.getContents())
                .filter(item -> item != null && RED_MATERIALS.contains(item.getType()))
                .count();

        double progress = 1.0 - ((double) totalInjuries / COMMON_INJURIES.size());
        bar.setProgress(Math.min(progress, 1.0));
    }

    private void endRevive(Player healer, Player downed) {
        healer.closeInventory();

        BossBar bar = downedBossBars.remove(downed.getUniqueId());
        if (bar != null) bar.removeAll();

        healerToDownedPlayer.remove(healer.getUniqueId());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player healer)) return;
        if (!e.getView().title().toString().contains("Reviving")) return;

        e.setCancelled(true);
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null) return;

        Inventory inv = e.getInventory();
        Player downed = healerToDownedPlayer.get(healer.getUniqueId());
        if (downed == null) return;

        // Distance check
        if (healer.getLocation().distance(downed.getLocation()) > 4.0) {
            endRevive(healer, downed);
            healer.sendMessage(Component.text("You are too far away! Revive cancelled.", NamedTextColor.RED));
            return;
        }

        Material type = clicked.getType();
        if (HEALTHY_MATERIALS.contains(type)) {
            addRandomInjuries(inv, 3);
            healer.playSound(healer.getLocation(), Sound.ENTITY_VILLAGER_HURT, 1, 0.8f);
            return;
        }

        // Replace injury with bandage
        inv.setItem(e.getSlot(), createBandageItem());
        healer.playSound(healer.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1, 1.4f);

        // Update bossbar progress
        updateBossBarProgress(downed, inv);


        if (allInjuriesCleared(inv)) {
            // Success: revive
            downed.getPersistentDataContainer().set(
                    new NamespacedKey(Specialization.getInstance(), "is_downed"),
                    PersistentDataType.BYTE,
                    (byte) 0
            );
            endRevive(healer, downed);

        }
    }

    private boolean allInjuriesCleared(Inventory inv) {
        return Arrays.stream(inv.getContents())
                .filter(Objects::nonNull)
                .noneMatch(item -> item.getItemMeta() != null
                        && item.getItemMeta().getPersistentDataContainer().has(INJURY_KEY, PersistentDataType.BYTE));
    }

    // inside your ReviveListener
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player healer)) return;

        Inventory inv = e.getInventory();
        if (!e.getView().title().toString().contains("Reviving")) return;

        Player downed = healerToDownedPlayer.get(healer.getUniqueId());
        if (downed == null) return;

        // Check if all injuries are cleared
        boolean done = Arrays.stream(inv.getContents())
                .filter(Objects::nonNull)
                .noneMatch(item -> item.getItemMeta() != null
                        && item.getItemMeta().getPersistentDataContainer().has(INJURY_KEY, PersistentDataType.BYTE));

        if (!done) {
            // Revive failed / incomplete
            BossBar bar = downedBossBars.remove(downed.getUniqueId());
            if (bar != null) bar.removeAll();

            healerToDownedPlayer.remove(healer.getUniqueId());
        }
    }


    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        // Clean up lingering bossbars
        UUID uuid = e.getPlayer().getUniqueId();
        BossBar bar = downedBossBars.remove(uuid);
        if (bar != null) bar.removeAll();
    }
}
