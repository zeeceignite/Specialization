package com.minecraftcivilizations.specialization.Combat;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import com.minecraftcivilizations.specialization.Listener.Player.Inventories.SpecializationCraftItemEvent;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.Specialization;
import com.minecraftcivilizations.specialization.StaffTools.Debug;
import com.minecraftcivilizations.specialization.util.ItemStackUtils;
import com.minecraftcivilizations.specialization.util.MathUtils;
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerItemBreakEvent;
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

import java.awt.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles applying attributes to armor, such as speed
 * @author alectriciti
 */
public class ArmorEquipAttributes implements Listener {

    Specialization plugin;
    CombatManager manager;


    public static NamespacedKey WEIGHT_KEY;
    public static NamespacedKey ARROW_RESIST_KEY;

    public ArmorEquipAttributes(CombatManager manager){
        this.manager = manager;
        this.plugin = manager.plugin;
        WEIGHT_KEY = new NamespacedKey(plugin, "armor_weight");
//        namespaceKeyFactory();
        this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
        // Initialization in constructor
    }

    /**
     * Applies armor weight based on Blacksmith Level
     */
    @EventHandler
    public void onArmorCraft(SpecializationCraftItemEvent special_event){
        if(special_event.getSkillType()!= SkillType.BLACKSMITH)return;
        if(special_event.getSkillLevel()<2)return;
        CraftItemEvent event = special_event.getEvent();
        if(event.isCancelled())return;
        if(event.getResult()!= Event.Result.ALLOW)return;

        Player player = special_event.getPlayer();

        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType() == Material.AIR) return;

        ItemStack modified = current.clone();
        ItemMeta meta = modified.getItemMeta();
        int lvl = special_event.getSkillLevel();
        EquipmentSlot slot = ArmorStats.getSlot(current.getType());
        if(slot==null)return;

        double weight;
        double weight_modifier = 1.0;
        double material_weight = 0;
        double slot_weight = 0;
        Material mat = ArmorStats.getMaterialType(current.getType());
        if (mat == null) return; //not compatible

        material_weight = getMaterialWeight(mat);
        slot_weight = getSlotModifier(slot);

        boolean luck = player.hasPotionEffect(PotionEffectType.LUCK);

        weight = (material_weight * slot_weight);
        ChatColor color = BLUE;
        double base_chance = 0.25;
        double rare_chance = 0.125;
        double weight_mod_low = 0.75;
        double weight_mod_high = 0.75;
        boolean best = false;
        switch(lvl){
            case 2:
                base_chance = 0.01; // 1 in 5
                rare_chance = 0.00;
                weight_mod_low = 0.8;
                weight_mod_high = 0.95;
                break;
            case 3:
                base_chance = 0.25; // 1 in 4
                rare_chance = (luck?0.03:0.02); // ~0.5% chance of crafting best
                weight_mod_low = 0.7;
                weight_mod_high = 0.9;
                break;
            case 4:
                base_chance = 0.33; // 1 in 3
                rare_chance = (luck?0.07:0.03); // ~2.0% chance of crafting best
                weight_mod_low = 0.75;
                weight_mod_high = 0.8;
                break;
            case 5:
                base_chance = 0.5; // 1 in 2
                rare_chance = (luck?0.1:0.05); // ~5.0% chance of crafting best
                weight_mod_low = 0.6;
                weight_mod_high = 0.8;
                break;
        }
        if(ThreadLocalRandom.current().nextDouble()<base_chance) {
            if (current.getType().name().contains("IRON_")) {
                if(ThreadLocalRandom.current().nextDouble()<rare_chance) {
                    color = WHITE;
                    weight_modifier = 0.425;
                    best = true;
                }else{
                    weight_modifier = MathUtils.random(weight_mod_low,weight_mod_low);
                }
            }else if(current.getType().name().contains("DIAMOND_")){
                if(ThreadLocalRandom.current().nextDouble()<rare_chance) {
                    color = AQUA;
                    weight_modifier = 0.5;
                    best = true;
                }else{
                    weight_modifier = MathUtils.random(weight_mod_low, 0.92);
                }
            }else if(current.getType().name().contains("GOLDEN_")){
                if(ThreadLocalRandom.current().nextDouble()<rare_chance) {
                    color = GOLD;
                    weight_modifier = 0.25;
                    best = true;
                }else{
                    weight_modifier = 0.5 * weight_mod_low;
                }
            }

            current = ArmorEquipAttributes.applyWeight(modified, (int) (weight * weight_modifier), color);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_WORK_TOOLSMITH, 0.2f, 0.9f);
            if(best){
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.22f, 1.1f);
            }
        }else{
            current = ArmorEquipAttributes.applyWeight(modified, -1, BLUE);
        }
        event.setCurrentItem(current);
    }


    /**
     * This displays Armor Weight to the player
     * It also converts items to have weight if it somehow has not yet been converted
     */
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

            // Optional filter to just armor slots
            switch (slot) {
                case HEAD, CHEST, LEGS, FEET -> {
                    if (newItem != null && !newItem.getType().isAir()) {
                        ItemStack modified = applyStats(newItem); // your method
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
    public ItemStack applyStats(ItemStack item){
        item = applyWeight(item, -1, ChatColor.BLUE);
//        item = applyArrowResist(item);
        return item;
    }
//    public ItemStack applyArrowResist(ItemStack item) {
//        ItemMeta meta = item.getItemMeta();
//        if (meta.getPersistentDataContainer().has(ARROW_RESIST_KEY)) return null; //returning null skips applying
//
//        double resist_value = 0;
//        switch (item.getType()) {
//            case CHAINMAIL_HELMET -> resist_value = 2;
//            case CHAINMAIL_CHESTPLATE -> resist_value = 4;
//            case CHAINMAIL_LEGGINGS -> resist_value = 3;
//            case CHAINMAIL_BOOTS -> resist_value = 2;
//        }
//
//        if(resist_value>0) {
//            meta.getPersistentDataContainer().set(ARROW_RESIST_KEY, PersistentDataType.DOUBLE, resist_value);
//            item.setItemMeta(meta);
//            ItemStackUtils.setLoreLine(item, 0, ChatColor.BLUE + "+" + resist_value+ " Arrow Protection");
//        }
//        return item;
//    }

    /**
     * Applies weight to an armor piece with a custom weight override
     * Used for custom blacksmith armor
     */
    public static ItemStack applyWeight(ItemStack item, double custom_weight, ChatColor color){
        ItemMeta meta = item.getItemMeta();
        if(meta.getPersistentDataContainer().has(WEIGHT_KEY))return null; //returning null skips applying


        // IRON TOUGHNESS OVERRIDE
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

            material_weight = getMaterialWeight(mat);
            slot_weight = getSlotModifier(slot);

            weight = (material_weight * slot_weight);
        }

        //apply weight
        meta.getPersistentDataContainer().set(WEIGHT_KEY, PersistentDataType.DOUBLE, weight);
        item.setItemMeta(meta);
        ItemStackUtils.setLoreLine(item, 0, color+"+"+weight+" Weight");
//        Debug.broadcast("weight", "weight applied! "+BLUE+"MAT: "+material_weight+" "+GREEN+"SLOT: "+slot_weight);
        return item;
    }

    public static double getMaterialWeight(Material mat) {
        double material_weight = 0;
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
        return material_weight;
    }

    public static double getSlotModifier(EquipmentSlot slot) {
        double slot_weight = 0;
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
        return slot_weight;
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



}
