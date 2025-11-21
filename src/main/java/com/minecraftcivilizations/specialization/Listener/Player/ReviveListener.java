package com.minecraftcivilizations.specialization.Listener.Player;

import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.Specialization;
import com.minecraftcivilizations.specialization.StaffTools.Debug;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/** @author Jfrogy*/

public class ReviveListener implements Listener {

    private static final NamespacedKey INJURY_KEY =
            new NamespacedKey(Specialization.getInstance(), "revive_injury");
    private static final List<InjuryItem> INJURIES = List.of(
            new InjuryItem("Tumor", Material.SPIDER_EYE),
            new InjuryItem("Blood Clout", Material.REDSTONE),
            new InjuryItem("Severe Bleeding", Material.RED_DYE),
            new InjuryItem("Damaged Muscle", Material.BEEF),
            new InjuryItem("Infected Injury", Material.NETHER_WART),
            new InjuryItem("Damaged Brain", Material.NETHER_WART_BLOCK),
            new InjuryItem("Broken Bones", Material.BONE_MEAL)
    );
    private static final List<HealthyItem> HEALTHY_ITEMS = List.of(
            new HealthyItem("Healthy Liver", Material.FERMENTED_SPIDER_EYE),
            new HealthyItem("Healthy Stomach", Material.RABBIT_FOOT),
            new HealthyItem("Healthy Heart", Material.BEETROOT),
            new HealthyItem("Healthy Kidneys", Material.SWEET_BERRIES),
            new HealthyItem("Healthy Brain", Material.RED_GLAZED_TERRACOTTA),
            new HealthyItem("Healthy Bone", Material.BONE)
    );

    private final PlayerDownedListener playerDownedListener;

    private final Map<UUID, AttributeModifier> healerSlowModifiers = new ConcurrentHashMap<>();
    private final Map<UUID, Player> healerToDownedPlayer = new HashMap<>();
    private final Map<UUID, BossBar> downedBossBars = new HashMap<>();

    public ReviveListener(PlayerDownedListener playerDownedListener) {
        this.playerDownedListener = playerDownedListener;
    }


    // -------------------------------
    // START REVIVE
    // -------------------------------
    public void startRevive(Player healer, Player downed, Inventory inv) {
        healer.openInventory(inv);
        healer.playSound(healer.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1.2f);
        healerToDownedPlayer.put(healer.getUniqueId(), downed);

        BossBar bar = Bukkit.createBossBar("Revive Progress", BarColor.GREEN, BarStyle.SOLID);
        bar.addPlayer(downed);
        bar.setProgress(0.0);
        downedBossBars.put(downed.getUniqueId(), bar);
    }

    // -------------------------------
// CREATE REVIVE INVENTORY
// -------------------------------
    public Inventory createReviveInventory(Player downed) {
        Inventory inv = Bukkit.createInventory(
                null, 54,
                Component.text("Reviving " + downed.getName(), NamedTextColor.BLACK)
        );

        // shuffle & place injuries
        List<InjuryItem> injuries = new ArrayList<>(INJURIES);
        Collections.shuffle(injuries);

        for (InjuryItem inj : injuries) {
            if (hasItemInInventory(inv, inj.mat())) continue; // skip if already present
            Integer slot = getRandomEmptySlot(inv);
            if (slot == null) break;
            inv.setItem(slot, createInjuryItem(inj));
        }

        // place healthy items
        List<HealthyItem> healthyList = new ArrayList<>(HEALTHY_ITEMS);
        Collections.shuffle(healthyList);

        for (HealthyItem h : healthyList) {
            if (hasItemInInventory(inv, h.mat())) continue;
            Integer slot = getRandomEmptySlot(inv);
            if (slot == null) break;
            inv.setItem(slot, createHealthyItem(h));
        }

        return inv;
    }

    private boolean hasItemInInventory(Inventory inv, Material mat) {
        return Arrays.stream(inv.getContents())
                .filter(Objects::nonNull)
                .anyMatch(i -> i.getType() == mat);
    }

    // Updated createHealthyItem to allow passing a specific HealthyItem
    private ItemStack createHealthyItem(HealthyItem h) {
        ItemStack item = new ItemStack(h.mat());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(h.name(), NamedTextColor.GREEN));
        item.setItemMeta(meta);
        return item;
    }


    private Integer getRandomEmptySlot(Inventory inv) {
        List<Integer> empty = new ArrayList<>();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType().isAir()) {
                empty.add(i);
            }
        }
        if (empty.isEmpty()) return null;
        return empty.get(ThreadLocalRandom.current().nextInt(empty.size()));
    }

    // -------------------------------
    // CREATE ITEMS
    // -------------------------------
    private ItemStack createInjuryItem(InjuryItem inj) {
        ItemStack item = new ItemStack(inj.mat());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(inj.name(), NamedTextColor.RED));
        meta.getPersistentDataContainer().set(INJURY_KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createHealthyItem() {
        HealthyItem h = HEALTHY_ITEMS.get(ThreadLocalRandom.current().nextInt(HEALTHY_ITEMS.size()));
        ItemStack item = new ItemStack(h.mat());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(h.name(), NamedTextColor.GREEN));
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

    // -------------------------------
    // REROLL INJURIES
    // -------------------------------
    private void addRandomInjuries(Inventory inv, int amount) {
        List<InjuryItem> shuffled = new ArrayList<>(INJURIES);
        Collections.shuffle(shuffled);

        int placed = 0;
        for (InjuryItem inj : shuffled) {
            if (placed >= amount) break;
            if (hasItemInInventory(inv, inj.mat())) continue;
            Integer slot = getRandomEmptySlot(inv);
            if (slot == null) break;
            inv.setItem(slot, createInjuryItem(inj));
            placed++;
        }
    }


    // -------------------------------
    // BOSSBAR PROGRESS
    // -------------------------------
    private void updateBossBarProgress(Player downed, Inventory inv) {
        BossBar bar = downedBossBars.get(downed.getUniqueId());
        if (bar == null) return;

        long totalInjuries = Arrays.stream(inv.getContents())
                .filter(Objects::nonNull)
                .filter(item -> item.hasItemMeta())
                .filter(item -> item.getItemMeta().getPersistentDataContainer().has(INJURY_KEY, PersistentDataType.BYTE))
                .count();

        double progress = 1.0 - ((double) totalInjuries / INJURIES.size());
        progress = Math.min(1.0, Math.max(0.0, progress));

        bar.setProgress(progress);
    }

    // -------------------------------
    // END REVIVE
    // -------------------------------
    private void endRevive(Player healer, Player downed) {
        healer.closeInventory();

        //indirectly revives the player lol
        downed.getPersistentDataContainer().set(
                new NamespacedKey(Specialization.getInstance(), "is_downed"),
                PersistentDataType.BYTE,
                (byte) 0
        );

        BossBar bar = downedBossBars.remove(downed.getUniqueId());
        if (bar != null) bar.removeAll();

        healerToDownedPlayer.remove(healer.getUniqueId());

    }

    // -------------------------------
    // INVENTORY CLICK HANDLER
    // -------------------------------
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player healer)) return;

        String title = PlainTextComponentSerializer.plainText().serialize(e.getView().title());
        if (!title.startsWith("Reviving ")) return;

        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null) return;

        Inventory inv = e.getInventory();
        Player downed = healerToDownedPlayer.get(healer.getUniqueId());
        if (downed == null) return;

        if (healer.getLocation().distanceSquared(downed.getLocation()) > 4.0) {
            endRevive(healer, downed);
            healer.sendMessage(Component.text("You are too far away! Revive cancelled.", NamedTextColor.RED));
            return;
        }

        Material type = clicked.getType();

        // Clicked healthy item → spawn more injuries
        boolean isHealthy = HEALTHY_ITEMS.stream().anyMatch(h -> h.mat() == type);
        if (isHealthy) {
            addRandomInjuries(inv, 3);
            healer.playSound(healer.getLocation(), Sound.ENTITY_VILLAGER_HURT, 1, 0.8f);
            return;
        }

        // Injury clicked → replace with bandage
        inv.setItem(e.getSlot(), createBandageItem());
        healer.playSound(healer.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1, 1.4f);
        updateBossBarProgress(downed, inv);

        if (allInjuriesCleared(inv)) {
            CustomPlayer cHealer = CoreUtil.getPlayer(healer.getUniqueId());
            int skillLevel = cHealer.getSkillLevel(SkillType.HEALER);
            int hearts = skillLevel * 2;

            healer.setHealth (Math.round(hearts));

            endRevive(healer, downed);
        }
    }

    private boolean isHealer(Player player) {
        CustomPlayer cHealer = CoreUtil.getPlayer(player.getUniqueId());
        int lvl = cHealer.getSkillLevel(SkillType.HEALER);
        if (lvl == 0 && !player.isOp()) return false;
        return true;
    }

    private boolean allInjuriesCleared(Inventory inv) {
        return Arrays.stream(inv.getContents())
                .filter(Objects::nonNull)
                .noneMatch(item ->
                        item.hasItemMeta()
                                && item.getItemMeta().getPersistentDataContainer().has(INJURY_KEY, PersistentDataType.BYTE)
                );
    }

    // -------------------------------
    // CLOSE INVENTORY = CANCEL UNLESS FINISHED
    // -------------------------------
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player healer)) return;

        if (!e.getView().title().toString().contains("Reviving")) return;

        Player downed = healerToDownedPlayer.get(healer.getUniqueId());
        if (downed == null) return;

        if (!allInjuriesCleared(e.getInventory())) {
            BossBar bar = downedBossBars.remove(downed.getUniqueId());
            if (bar != null) bar.removeAll();

            healerToDownedPlayer.remove(healer.getUniqueId());
        }
    }

    // -------------------------------
// PICKUP PASSENGER
// -------------------------------
    @EventHandler
    public void onPickupPassenger(PlayerInteractAtEntityEvent e) {
        Player healer = e.getPlayer();
        Entity target = e.getRightClicked();
        if (!(target instanceof LivingEntity)) return; //must be a living entity

        Byte targetdowned = target.getPersistentDataContainer().get(new NamespacedKey(Specialization.getInstance(), "is_downed"), PersistentDataType.BYTE);
        if ((targetdowned == null || targetdowned == 0)) return; //target must be down

        Byte healerdowned = target.getPersistentDataContainer().get(new NamespacedKey(Specialization.getInstance(), "is_downed"), PersistentDataType.BYTE);
        if (!(healerdowned == null || healerdowned == 0)) return; //healer must NOT be downed

        if (!isHealer(healer)) return; //must be healer or op
        if (!healer.getPassengers().isEmpty()) return; // must not have any passangers already
        if (!healer.getInventory().getItemInMainHand().getType().isAir()) return; // must be empty hand
        if (healer.getPassengers().contains(target)) return; // already a passenger


        if (target instanceof  Player downedplayer) {
        playerDownedListener.clearMount(downedplayer);
        }

        removeSlowIfNoPassengers(healer, false);
        healer.addPassenger(target);
        NamespacedKey key = new NamespacedKey("revive", "slow_" + healer.getUniqueId());
        AttributeModifier slow = new AttributeModifier(key, -0.5, AttributeModifier.Operation.MULTIPLY_SCALAR_1);

        if (healer.getAttribute(Attribute.MOVEMENT_SPEED) != null) {
            healer.getAttribute(Attribute.MOVEMENT_SPEED).addModifier(slow);
            healerSlowModifiers.put(healer.getUniqueId(), slow);
        }

        Debug.broadcast("revive", "You are now carrying " + target.getName());
    }


    // -------------------------------
// REMOVE PASSENGER ON SNEAK
// -------------------------------
    @EventHandler
    public void onDismountSneak(PlayerToggleSneakEvent e) {
        Player healer = e.getPlayer();
        if (e.getPlayer().getVehicle() instanceof Player){
            e.setCancelled(true);
        }
        if (!healer.isSneaking()) return;

        for (Entity passenger : new ArrayList<>(healer.getPassengers())) {
            healer.removePassenger(passenger);
            Byte downed = passenger.getPersistentDataContainer().get(
                    new NamespacedKey(Specialization.getInstance(), "is_downed"),
                    PersistentDataType.BYTE
            );

            boolean isDowned = downed != null && downed == 1;
            if(isDowned && passenger instanceof Player rider) {
            playerDownedListener.setSit(rider);

            }

        }

        removeSlowIfNoPassengers(healer,false);
    }

    @EventHandler
    public void onDismountEvent(EntityDismountEvent e) {
        if (!(e.getDismounted() instanceof Player p)) return;

        Byte downed = p.getPersistentDataContainer().get(
                new NamespacedKey(Specialization.getInstance(), "is_downed"),
                PersistentDataType.BYTE
        );

        boolean isDowned = downed != null && downed == 1;
        boolean riddenPlayerIsSneaking = p.isSneaking();

        // If player is downed AND the ridden player is NOT sneaking → block dismount
        if (isDowned && !riddenPlayerIsSneaking) {
            e.setCancelled(true);
        }

        //removes the slow from the carrier if they dismount a downedplayer
        if (!isDowned) {
            Entity carryingEntity = e.getDismounted();
            if (carryingEntity instanceof Player carrierPlayer) {
                removeSlowIfNoPassengers(carrierPlayer, true);
            }
        }
    }


    private void removeSlowIfNoPassengers(Player healer, boolean override) {
        if (healer.getPassengers().isEmpty() || override) {
            AttributeModifier slow = healerSlowModifiers.remove(healer.getUniqueId());
            if (slow != null && healer.getAttribute(Attribute.MOVEMENT_SPEED) != null) {
                healer.getAttribute(Attribute.MOVEMENT_SPEED).removeModifier(slow);
                Debug.broadcast("revive", "Removed slowed attribute for" + healer.getName());
            }
        }
    }



    // -------------------------------
// PLAYER LEAVE / DEATH CLEANUP
// -------------------------------
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        removeSlowIfNoPassengers(p, false);
        if (p.getVehicle() instanceof Player vehicle){
            p.sendMessage("quitter");
            vehicle.removePassenger(p);
    }

        BossBar bar = downedBossBars.remove(p.getUniqueId());
        if (bar != null) bar.removeAll();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        removeSlowIfNoPassengers(p, false);

        BossBar bar = downedBossBars.remove(p.getUniqueId());
        if (bar != null) bar.removeAll();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        removeSlowIfNoPassengers(p, false);

        BossBar bar = downedBossBars.remove(p.getUniqueId());
        if (bar != null) bar.removeAll();


    }

    // --- INJURY ITEMS ---
    public record InjuryItem(String name, Material mat) {
    }

    // --- HEALTHY ITEMS ---
    public record HealthyItem(String name, Material mat) {
    }
}
