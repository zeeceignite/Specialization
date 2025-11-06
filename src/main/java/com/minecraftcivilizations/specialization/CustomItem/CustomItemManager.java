package com.minecraftcivilizations.specialization.CustomItem;

import com.minecraftcivilizations.specialization.Specialization;
import com.minecraftcivilizations.specialization.StaffTools.Debug;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 *
 * @author  alectriciti, jfrogy
 */
public class CustomItemManager implements Listener {

    static NamespacedKey key_custom_item_id = new NamespacedKey("specialization", "custom_item_id");
    //This holds ALL custom items, even ones that are not enabled
//    public Set<CustomItem> custom_items_to_register = new HashSet<CustomItem>(); // For Loading ONLY

    //Holds registered custom items
    public Map<String, CustomItem> custom_items_loaded = new HashMap<String, CustomItem>();

    //Used by Custom Item Command for Tab Completion
    @Getter
    public List<String> customItemIds = new ArrayList<String>();


    DefineCustomItems definitions;

    public CustomItemManager(Specialization plugin){
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void initializeCustomItems(){
        custom_items_loaded = new HashMap<String, CustomItem>();
        customItemIds = new ArrayList<String>();
        definitions = new DefineCustomItems();
        for(CustomItem customItem : custom_items_loaded.values()){
            customItem.init();
        }
    }

    public static CustomItemManager getInstance() {
        return Specialization.getInstance().getCustomItemManager();
    }


    void registerItem(CustomItem customItem) {
        Specialization.getInstance().getLogger().info("Registering Custom Item: "+customItem.getId());
//        custom_items_to_register.add(customItem);
        custom_items_loaded.put(customItem.getId(), customItem);
        customItemIds.add(customItem.getId());
    }

    /**
     * Called when disabling an item
     */
//    void unregisterItem(CustomItem customItem) {
//        custom_items_loaded.remove(customItem.getId());
//        customItemIds.remove(customItem.getId());
//    }


//    public void finalizeCustomItemRegistration() {
//        for(CustomItem item : custom_items_to_register){
//            if(item.isEnabled()){
//                custom_items_loaded.put(item.getId(), item);
//            }
//        }
//    }

    /**
     * The primary way to get a Custom Item via Interactions
     */
    public CustomItem getCustomItem(ItemStack item){
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if(meta!=null){
            String id = meta.getPersistentDataContainer().get(key_custom_item_id, PersistentDataType.STRING);
            if(id!=null){
                CustomItem ci = custom_items_loaded.get(id);
                return ci;
            }
        }
        return null;
    }

    /**
     * Gets Custom Item, used by commands
     */
    public CustomItem getCustomItem(String id){
        if(custom_items_loaded.containsKey(id)){
            return custom_items_loaded.get(id);
        }
        return null;
    }

    public boolean disableItem(String id) {
        CustomItem item = custom_items_loaded.get(id);
        if (item == null) return false;
        item.setEnabled(false);
        return true;
    }

    public boolean enableItem(String id) {
        CustomItem item = custom_items_loaded.get(id);
        if (item == null) return false;
        item.setEnabled(true);
        return true;
    }

    public void reloadItem(String id) {
        CustomItem item = custom_items_loaded.get(id);
        if (item == null) return;

        item.init();

        // Invalidate or rebuild ItemStack model data if needed.
        Specialization.getInstance().getLogger().info("Refreshed custom item: " + id);
    }

    @EventHandler
    public void onItemCreation(CustomItemCreationEvent event){
        String by = "";
        Player player = event.getPlayer();
        if(player != null) {
            if(!event.getCustomItemClass().isEnabled()){
                if(!player.isOp()) {
                    event.setCancelled(true);
                }
            }
            by = "by "+event.getPlayer().getName();
        }
           Debug.broadcast("customitem",
                   "Custom Item "+event.getCustomItemClass().getId()+" created "+by+(event.isCancelled()?ChatColor.RED+"[Cancelled]":""));
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event){
        ItemStack itemstack = event.getItem();
        if(itemstack != null){
            CustomItem custom_item = getCustomItem(itemstack);
            if(custom_item!=null) {
                if (custom_item.isEnabled() || event.getPlayer().isOp()) {
                    custom_item.onInteract(event, itemstack);
                }
            }
        }

    }


    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        ItemStack new_item = event.getPlayer().getInventory().getItem(event.getNewSlot());
        if (new_item != null) {
            CustomItem custom = getCustomItem(new_item);
            if (custom != null) {
                custom.onItemSwitchTo(event, new_item);
            }
        }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        ItemStack is = event.getPlayer().getEquipment().getItem(event.getHand());
        CustomItem custom_item = getCustomItem(is);
        if (custom_item != null) {
            if(custom_item.isEnabled() || event.getPlayer().isOp()) {
                custom_item.onInteractEntity(event, is);
            }
        }
    }



    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        CustomItem custom = getCustomItem(item);
        if (custom != null) {
            custom.onBlockBreak(event);
        }
    }
}
