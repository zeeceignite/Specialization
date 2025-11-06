package com.minecraftcivilizations.specialization.Listener.Player.Combat;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import com.minecraftcivilizations.specialization.Specialization;
import com.minecraftcivilizations.specialization.StaffTools.Debug;
import com.minecraftcivilizations.specialization.util.ItemStackUtils;
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import static net.md_5.bungee.api.ChatColor.*;
import static org.bukkit.ChatColor.BLUE;
import static org.bukkit.ChatColor.WHITE;

import java.awt.*;
import java.util.*;

/**
 * Handles applying attributes to armor, such as speed
 * @author alectriciti
 */
public class ArmorEquipAttributes implements Listener {

    Specialization plugin;
    CombatManager manager;


    public static NamespacedKey WEIGHT_KEY;

    public ArmorEquipAttributes(CombatManager manager){
        this.manager = manager;
        this.plugin = manager.plugin;
        WEIGHT_KEY = new NamespacedKey(plugin, "item_weight");
//        namespaceKeyFactory();
        this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
        // Initialization in constructor
    }



    @EventHandler
    public void onArmorChange(EntityEquipmentChangedEvent event) {
        // Only care about players
        if (!(event.getEntity() instanceof Player player)) return;

//        UUID uuid = player.getUniqueId();
//        double old_weight = player_weight_history.getOrDefault(uuid, weight_offset);

        EntityEquipment equipment = player.getEquipment();

        boolean armor_swap = false;

        for (Map.Entry<EquipmentSlot, EntityEquipmentChangedEvent.EquipmentChange> entry
                : event.getEquipmentChanges().entrySet()) {


            EquipmentSlot slot = entry.getKey();
            EntityEquipmentChangedEvent.EquipmentChange change = entry.getValue();
            ItemStack oldItem = change.oldItem();
            ItemStack newItem = change.newItem();
// treat null or AIR as empty
            boolean oldEmpty = oldItem.getType() == Material.AIR;
            boolean newEmpty = newItem.getType() == Material.AIR;

// ignore no-op changes
            if (oldEmpty && newEmpty) continue;
            if (!oldEmpty && !newEmpty && oldItem.isSimilar(newItem)) continue;

            if(oldItem.getType().equals(newItem.getType()))continue;

            // Optional filter to just armor slots
            switch (slot) {
                case HEAD, CHEST, LEGS, FEET -> {
                    if (newItem != null && !newItem.getType().isAir()) {
                        ItemStack modified = applyWeight(newItem); // your method
                        armor_swap = true;
                        if(modified!=null) {
                            equipment.setItem(slot, modified);
                        }
                    }
                }
                default -> {}
            }
        }

        if(armor_swap) {
            double weight = calculateWeight(player);
//            if (old_weight != weight) {
//        Debug.broadcast("weight", "New Weight: "+weightColor(weight)+weight);
                player.sendActionBar("Armor Weight: " + weightColor(weight) + weight);
            ArmorStats stats = ArmorStats.getArmorStats(player.getEquipment());
            Debug.broadcast("armorstats", BLUE+"Armor: "+WHITE+stats.getArmor()+BLUE+" Toughness: "+WHITE+stats.getToughness());
//        player.updateInventory();
//            }
//            player_weight_history.put(uuid, weight);
        }
    }

    public final double weight_offset = -25; //baseline, a player can have up to this before weight becomes effective
    public final double weight_threshold = 50;

    public ChatColor weightColor(double weight){
        if(weight> weight_threshold *3){
            return DARK_RED;
        }else if(weight> weight_threshold *2){
            return RED;
        }else if (weight> weight_threshold){
            return GOLD;
        }else if(weight>0){
            return YELLOW;
        }
        return GREEN;
    }

    public boolean hasWeight(ItemStack item){
        return item.getItemMeta().getPersistentDataContainer().has(WEIGHT_KEY);
    }

    public double getWeight(ItemStack item){
        if(item == null || item.getItemMeta() == null) return 0;
        if(item.getItemMeta().getPersistentDataContainer().has(WEIGHT_KEY)) {
            return item.getItemMeta().getPersistentDataContainer().get(WEIGHT_KEY, PersistentDataType.DOUBLE);
        }
        return 0;
    }

    /**
     * Applies the default weight to an item based on material/slot combination
     * Used for default armor
     */
    public ItemStack applyWeight(ItemStack item){
        return applyWeight(item, -1);
    }

    /**
     * Applies weight to an armor piece with a custom weight override
     * Used for custom blacksmith armor
     */
    public ItemStack applyWeight(ItemStack item, double custom_weight){
        ItemMeta meta = item.getItemMeta();
        if(meta.getPersistentDataContainer().has(WEIGHT_KEY))return null; //returning null skips applying


        // IRON TOUGHNESS
        if(item.getType().name().contains("IRON_")){
            AttributeModifier mod_armor = new AttributeModifier(
                    new NamespacedKey(Specialization.getInstance(), item.getType().name().toLowerCase()+"_armor"),
                    ArmorStats.getVanillaStats(item.getType()).getArmor(),
                    AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ARMOR);
            meta.addAttributeModifier(Attribute.ARMOR, mod_armor);
            AttributeModifier mod_tough = new AttributeModifier(
                    new NamespacedKey(Specialization.getInstance(), item.getType().name().toLowerCase()+"_toughness"),
                    1,
                    AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ARMOR);
            meta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, mod_tough);
        }

        double weight; //weight to apply to the item
        if(custom_weight!=-1) {
            weight = custom_weight; //manually apply weight
        }else{
            double material_weight = 0;
            double slot_weight = 0;
            EquipmentSlot slot = ArmorStats.getSlot(item.getType());
            Material mat = ArmorStats.getMaterialType(item.getType());
            if (mat == null) return item; //not compatible

            switch (mat) {
                case LEATHER:
                    material_weight = 1.0;
                    break;
                case CHAIN:
                    material_weight = 2.0;
                    break;
                case TURTLE_SCUTE:
                    material_weight = 2.0;
                    break;
                case GOLD_INGOT:
                    material_weight = 2.5;
                    break;
                case IRON_INGOT:
                    material_weight = 3.0;
                    break;
                case DIAMOND:
                    material_weight = 5.0;
                    break;
                case NETHERITE_INGOT:
                    material_weight = 6.0;
                    break;
            }
            switch (slot) {
                case HEAD -> {
                    slot_weight = 5.0;
                }
                case CHEST -> {
                    slot_weight = 8.0;
                }
                case LEGS -> {
                    slot_weight = 7.0;
                }
                case FEET -> {
                    slot_weight = 4.0;
                }
            }


            weight = (material_weight * slot_weight);
        }

        //apply weight
        meta.getPersistentDataContainer().set(WEIGHT_KEY, PersistentDataType.DOUBLE, weight);
        item.setItemMeta(meta);
        ItemStackUtils.setLoreLine(item, 1, weight_color+"Weight: "+WHITE+weight);
//        Debug.broadcast("weight", "weight applied! "+BLUE+"MAT: "+material_weight+" "+GREEN+"SLOT: "+slot_weight);
        return item;
    }

    public static ChatColor weight_color = ChatColor.of(new Color(172,172,122));

    private Map<UUID, Double> player_weight_history = new HashMap<UUID, Double>();


    private static final double BASE_WALK_SPEED = 0.1; // vanilla default
    private static final double MAX_ADDITIVE_BOOST = 0.6; // safety cap

    private static final EnumSet<EquipmentSlot> VALID_ARMOR_SLOTS = EnumSet.of(
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET);


    public double calculateWeight(Player player){
        double weight = 0.0 + weight_offset;
        EntityEquipment equipment = player.getEquipment();
        for(EquipmentSlot slot : VALID_ARMOR_SLOTS){
            ItemStack item = equipment.getItem(slot);
            weight += getWeight(item);
        }
        return weight;
    }

    @EventHandler
    public void onPlayerJump(PlayerJumpEvent event) {
        Player player = event.getPlayer();
        if(!player.isSprinting())return;
        Vector v = player.getVelocity();
        double y = v.getY();


        double weight = calculateWeight(player);

        if(weight>0) {
            int level = (int) ( weight / 50);
//            Debug.broadcast("jump", player.getName() + " weight: " + weight + " slowness: " + (level + 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 15 + (5 * level), level, false, false, false));
        }else{
//            Debug.broadcast("jump", player.getName() + " weight: " + weight + GRAY+" nothing applied");
        }
    }




//    public enum ArmorAttribute { MOVEMENT_SPEED, SWIM_SPEED, MAX_HEALTH, JUMP_HEIGHT, ARMOR, TOUGHNESS, KNOCKBACK }
//
//    private Map<Pair<ArmorAttribute, EquipmentSlot>, NamespacedKey> keys = new EnumMap<>(Pair.class);
//
//    private void namespaceKeyFactory(){
//        for (ArmorAttribute attr : ArmorAttribute.values()) {
//            for (EquipmentSlot slot : EquipmentSlot.values()) {
//                keys.put(Pair.of(attr, slot),
//                    new NamespacedKey(plugin, attr.name().toLowerCase() + "_" + slot.name().toLowerCase()));
//            }
//        }
//    }
//
//    public NamespacedKey getKey(ArmorAttribute attr, EquipmentSlot slot) {
//        return keys.get(Pair.of(attr, slot));
//    }


//    @EventHandler
//    public void onItemCreate(PrepareItemCraftEvent event){
//        ItemStack item = event.getInventory().getResult();
//        if(){
//
//        }
//    }

    /**
     * Called to apply attributes to armor
     * //@param force_apply_anyway if true, it will reapply the attribute.
     */

//    private ItemStack applyStats(ItemStack item) {
//        ItemMeta meta = item.getItemMeta();
//
//        double slot_weight = 0;
//        EquipmentSlot slot = ArmorStats.getSlot(item.getType());
//        if(slot==null){
//            Debug.broadcast("armor", "returning early for "+item.getType());
//            return null;
//        }
//
//        //determine if slot has pre-existing data
//
//        if(meta.hasAttributeModifiers()){
//            //armor might have for example, +7 armor
//            Multimap<Attribute, AttributeModifier> attributeModifiers = meta.getAttributeModifiers();
//            for(Attribute a : attributeModifiers.keySet()){
//                String string = "";
//                if(attributeModifiers.get(a) instanceof AttributeModifier mod){
//                    string = " modifier: "+mod.getName()+" amount: "+mod.getAmount();
//                }
//                Debug.broadcast("attribute", "attribute: "+a.getKey()+" "+string);
//            }
//        }
//
////        if(key==null){
////            Debug.broadcast("armor", "key is NULL");
////            return null;
////        }
//
//
//        switch(slot){
//            case HEAD -> {
//                slot_weight = 0.1;
//            }
//            case CHEST -> {
//                slot_weight = 0.25;
//            }
//            case LEGS -> {
//                slot_weight = 0.2;
//            }
//            case FEET -> {
//                slot_weight = 0.1;
//            }
//        }
//
//        ArmorStats vanilla_stats = ArmorStats.getStats(item.getType());
//        boolean add_armor = false, add_toughness = false;
//        if(meta.hasAttributeModifiers()){
//
//
//            AttributeModifier remove = null;
//            meta.getAttributeModifiers(Attribute.ARMOR);
//            Collection<AttributeModifier> attributeModifiers = meta.getAttributeModifiers().get(Attribute.WATER_MOVEMENT_EFFICIENCY);
//            for(AttributeModifier mod : attributeModifiers){
//                assert mod != null;
//                if(mod.getKey().equals(key)){
//                    remove = mod;
//                    break;
//                }
//            }
//            if(remove != null){
////                meta.removeAttributeModifier(Attribute.MOVEMENT_SPEED, remove);
//                meta.removeAttributeModifier(Attribute.WATER_MOVEMENT_EFFICIENCY, remove);
////                meta.removeAttributeModifier(Attribute.JUMP_STRENGTH, remove);
//            }
//            Collection<AttributeModifier> armormods = meta.getAttributeModifiers().get(Attribute.ARMOR);
//            if(armormods.size()==0){
//                add_armor = true;
//            }
//            Collection<AttributeModifier> armortoughnessmods = meta.getAttributeModifiers().get(Attribute.ARMOR_TOUGHNESS);
//            if(armortoughnessmods.size()==0){
//                add_toughness = true;
//            }
//
//        }else{
//            add_armor = true;
//            add_toughness = true;
//        }
//
//        if(add_armor){
//            meta.addAttributeModifier(Attribute.ARMOR, new AttributeModifier(
//                    getKeyBySlot(Type.ARMOR, slot),
//                    vanilla_stats.getArmor(),
//                    AttributeModifier.Operation.ADD_NUMBER,
//                    slot.getGroup()));
//            Debug.broadcast("armor", "Vanilla Armor added: "+vanilla_stats.getArmor());
//        }
//        if(add_toughness){
//            meta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, new AttributeModifier(
//                    getKeyBySlot(Type.TOUGHNESS, slot),
//                    vanilla_stats.getToughness(),
//                    AttributeModifier.Operation.ADD_NUMBER,
//                    slot.getGroup()));
//
//            Debug.broadcast("armor", "Vanilla Toughness added: "+vanilla_stats.getToughness());
//        }
//
//
//        /**
//         * Start applying attributes
//         */
//
//        double material_weight = 0;
//        switch(ArmorStats.getMaterialType(item.getType())){
//            case LEATHER:
//                material_weight = 0.125;
//                break;
//            case CHAIN:
//                material_weight = 0.25;
//                break;
//            case GOLD_INGOT:
//            case IRON_INGOT:
//                material_weight = 0.25;
//                break;
//            case DIAMOND:
//                material_weight = 0.5;
//                break;
//            case NETHERITE_INGOT:
//                material_weight = 0.75;
//                break;
//        }
//
//        double value = - (material_weight * slot_weight);
//        Debug.broadcast("armor", "weight applied to "+item.getType()+": "+value);
//
//        AttributeModifier modifier = new AttributeModifier(key, value, AttributeModifier.Operation.ADD_SCALAR, slot.getGroup());
////        meta.addAttributeModifier(Attribute.MOVEMENT_SPEED, modifier);
//        meta.addAttributeModifier(Attribute.WATER_MOVEMENT_EFFICIENCY, modifier);
////        meta.addItemFlags(ItemFlag);
//        item.setItemMeta(meta);
//        return item;
//    }
//
//
//    private void handle(Player player) {
//        CustomPlayer customPlayer = CoreUtil.getPlayer(player);
//        for (ItemStack item : player.getInventory().getArmorContents()) {
//            if (item != null) applyStats(item);
//        }
//    }



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

//    @EventHandler
//    public void onInventoryDrag(InventoryDragEvent e) {
//        if (e.getWhoClicked() instanceof Player p) handle(p);
//    }
//
//    @EventHandler
//    public void onPlayerInteract(PlayerInteractEvent e) {
//        handle(e.getPlayer());
//    }
//
//    @EventHandler
//    public void onSwapHand(PlayerSwapHandItemsEvent e) {
//        handle(e.getPlayer());
//    }
//
//    @EventHandler
//    public void onPickup(EntityPickupItemEvent e) {
//        if (e.getEntity() instanceof Player p) handle(p);
//    }
//
//    @EventHandler
//    public void onItemBreak(PlayerItemBreakEvent e) {
//        handle(e.getPlayer());
//    }
//
//    @EventHandler
//    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent e) {
//        handle(e.getPlayer());
//    }
//
//    @EventHandler
//    public void onPlayerJoin(PlayerJoinEvent e) {
//        handle(e.getPlayer());
//    }





}
