package com.minecraftcivilizations.specialization.Listener.Player.Combat;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Specialization;
import com.minecraftcivilizations.specialization.StaffTools.Debug;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Handles applying attributes to armor, such as speed
 * @author alectriciti
 */
public class ArmorEquipAttributes implements Listener {

    Specialization plugin;
    CombatManager manager;

    public ArmorEquipAttributes(CombatManager manager){
        this.manager = manager;
        this.plugin = manager.plugin;
        namespaceKeyFactory();
        this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
        // Initialization in constructor
    }


    enum ArmorAttribute { SPEED, SWIM, ARMOR, TOUGHNESS, KNOCKBACK }

    private Map<Pair<ArmorAttribute, EquipmentSlot>, NamespacedKey> keys = new EnumMap<>(Pair.class);

    private void namespaceKeyFactory(){
        for (ArmorAttribute attr : ArmorAttribute.values()) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                keys.put(Pair.of(attr, slot),
                        new NamespacedKey(plugin, attr.name().toLowerCase() + "_" + slot.name().toLowerCase()));
            }
        }
        Map<Pair<ArmorAttribute, EquipmentSlot>, UUID> uuids = new HashMap<>();

        for (ArmorAttribute attr : ArmorAttribute.values()) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                uuids.put(Pair.of(attr, slot), UUID.nameUUIDFromBytes((attr + "-" + slot).getBytes()));
            }
        }
    }

    public NamespacedKey getKey(ArmorAttribute attr, EquipmentSlot slot) {
        return keys.get(Pair.of(attr, slot));
    }



    /**
     * Called to apply attributes to armor
     */
    private ItemStack applyStats(ItemStack item) {
        ItemMeta meta = item.getItemMeta();

        double slot_weight = 0;
        EquipmentSlot slot = ArmorStats.getSlot(item.getType());
        if(slot==null){
            Debug.broadcast("armor", "returning early for "+item.getType());
            return null;
        }


        for(ArmorAttribute attribute : ArmorAttribute.values()){


        }
        NamespacedKey key = getKey();
        switch(slot){
            case HEAD -> {
                slot_weight = 0.1;
            }
            case CHEST -> {
                slot_weight = 0.25;
            }
            case LEGS -> {
                slot_weight = 0.2;
            }
            case FEET -> {
                slot_weight = 0.1;
            }
        }
        if(key==null){
            Debug.broadcast("armor", "key is NULL");
            return null;
        }

        ArmorStats vanilla_stats = ArmorStats.getStats(item.getType());
        boolean add_armor = false, add_toughness = false;
        if(meta.hasAttributeModifiers()){
            AttributeModifier remove = null;
            meta.getAttributeModifiers(Attribute.ARMOR);
            Collection<AttributeModifier> attributeModifiers = meta.getAttributeModifiers().get(Attribute.WATER_MOVEMENT_EFFICIENCY);
            for(AttributeModifier mod : attributeModifiers){
                assert mod != null;
                if(mod.getKey().equals(key)){
                    remove = mod;
                    break;
                }
            }
            if(remove != null){
//                meta.removeAttributeModifier(Attribute.MOVEMENT_SPEED, remove);
                meta.removeAttributeModifier(Attribute.WATER_MOVEMENT_EFFICIENCY, remove);
//                meta.removeAttributeModifier(Attribute.JUMP_STRENGTH, remove);
            }
            Collection<AttributeModifier> armormods = meta.getAttributeModifiers().get(Attribute.ARMOR);
            if(armormods.size()==0){
                add_armor = true;
            }
            Collection<AttributeModifier> armortoughnessmods = meta.getAttributeModifiers().get(Attribute.ARMOR_TOUGHNESS);
            if(armortoughnessmods.size()==0){
                add_toughness = true;
            }

        }else{
            add_armor = true;
            add_toughness = true;
        }

        if(add_armor){
            meta.addAttributeModifier(Attribute.ARMOR, new AttributeModifier(
                    getKeyBySlot(Type.ARMOR, slot),
                    vanilla_stats.getArmor(),
                    AttributeModifier.Operation.ADD_NUMBER,
                    slot.getGroup()));
            Debug.broadcast("armor", "Vanilla Armor added: "+vanilla_stats.getArmor());
        }
        if(add_toughness){
            meta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, new AttributeModifier(
                    getKeyBySlot(Type.TOUGHNESS, slot),
                    vanilla_stats.getToughness(),
                    AttributeModifier.Operation.ADD_NUMBER,
                    slot.getGroup()));


            Debug.broadcast("armor", "Vanilla Toughness added: "+vanilla_stats.getToughness());
        }


        /**
         * Start applying attributes
         */

        double material_weight = 0;
        switch(ArmorStats.getMaterialType(item.getType())){
            case LEATHER:
                material_weight = 0.125;
                break;
            case CHAIN:
                material_weight = 0.25;
                break;
            case GOLD_INGOT:
            case IRON_INGOT:
                material_weight = 0.25;
                break;
            case DIAMOND:
                material_weight = 0.5;
                break;
            case NETHERITE_INGOT:
                material_weight = 0.75;
                break;
        }

        double value = - (material_weight * slot_weight);
        Debug.broadcast("armor", "weight applied to "+item.getType()+": "+value);

        AttributeModifier modifier = new AttributeModifier(key, value, AttributeModifier.Operation.ADD_SCALAR, slot.getGroup());
//        meta.addAttributeModifier(Attribute.MOVEMENT_SPEED, modifier);
        meta.addAttributeModifier(Attribute.WATER_MOVEMENT_EFFICIENCY, modifier);
//        meta.addItemFlags(ItemFlag);
        item.setItemMeta(meta);
        return item;
    }


    private void handle(Player player) {
        CustomPlayer customPlayer = CoreUtil.getPlayer(player);
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item != null) applyStats(item);
        }
    }


    @EventHandler
    public void onArmorChange(EntityEquipmentChangedEvent event) {
        // Only care about players
        if (!(event.getEntity() instanceof Player player)) return;

        EntityEquipment equipment = player.getEquipment();
        if (equipment == null) return;

        for (Map.Entry<EquipmentSlot, EntityEquipmentChangedEvent.EquipmentChange> entry
                : event.getEquipmentChanges().entrySet()) {

            EquipmentSlot slot = entry.getKey();
            EntityEquipmentChangedEvent.EquipmentChange change = entry.getValue();

            ItemStack oldItem = change.oldItem();
            ItemStack newItem = change.newItem();

            // Optional filter to just armor slots
            switch (slot) {
                case HEAD, CHEST, LEGS, FEET -> {
                    if (newItem != null && !newItem.getType().isAir()) {
                        ItemStack modified = applyStats(newItem); // your method
                        if(modified!=null) {
                            equipment.setItem(slot, modified);
                        }
                    }
                }
                default -> {}
            }
        }
        player.updateInventory();
    }

//    @EventHandler F this crap
//    public void onInventoryClick(InventoryClickEvent e) {
//        if(!(e.getWhoClicked() instanceof Player player))return;
//        ItemStack cursor = e.getCursor();       // item on cursor
//        ItemStack current = e.getCurrentItem(); // item in clicked slot
//        if(cursor!=null){
//            applyStats(cursor);
//        }
//        if(current!=null){
//            applyStats(current);
//        }
////        if (e.getWhoClicked() instanceof Player p) handle(p);
//    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (e.getWhoClicked() instanceof Player p) handle(p);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        handle(e.getPlayer());
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent e) {
        handle(e.getPlayer());
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player p) handle(p);
    }

    @EventHandler
    public void onItemBreak(PlayerItemBreakEvent e) {
        handle(e.getPlayer());
    }

    @EventHandler
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent e) {
        handle(e.getPlayer());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        handle(e.getPlayer());
    }



    private static final double BASE_WALK_SPEED = 0.1; // vanilla default
    private static final double MAX_ADDITIVE_BOOST = 0.6; // safety cap

    private static EnumSet<EquipmentSlot> valid_slots = EnumSet.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
    @EventHandler
    public void onPlayerJump(PlayerJumpEvent event) {
        Player player = event.getPlayer();
        if(!player.isSprinting())return;
        Vector v = player.getVelocity();
        double y = v.getY();

//        player.getAttr
//        Collection<AttributeModifier> modifiers = player.getAttribute(Attribute.WATER_MOVEMENT_EFFICIENCY).getModifiers();
        double speed_modifier = 0.1;

        for(EquipmentSlot slot : valid_slots){
            AttributeModifier modifier = player.getAttribute(Attribute.WATER_MOVEMENT_EFFICIENCY).getModifier(getKeyBySlot(slot));
            if(modifier != null){
                speed_modifier += modifier.getAmount();
            }
        }
//        Double current_speed = player.getAttribute(Attribute.MOVEMENT_SPEED).getModifier(SPEED_KEY).getAmount();
        int level = (int)(-(speed_modifier * 7));
        Debug.broadcast("jump", player.getName()+ " speed: "+speed_modifier+" level: "+level);
        if(speed_modifier<0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, level, false, false, false));
        }
    }




}
