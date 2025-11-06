package com.minecraftcivilizations.specialization.CustomItem;

import com.minecraftcivilizations.specialization.Specialization;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 *
 * @author  alectriciti, jfrogy
 */
public abstract class CustomItem {

    protected final String id;
    protected String displayName;
    protected Material material;
    protected String customModelData; // null = not used
    protected int maxStackSize;
    protected boolean enabled;
    protected long cooldownMillis;

    protected String permission;

    @Getter
    protected boolean craftable;

    // Per-player cooldown tracking
    private final Map<UUID, Long> lastUse = new HashMap<>();

    /**
     * Primaray Construct (always called)
     */
    public CustomItem(String id,
                      String displayName,
                      Material material,
                      String customModelData,
                      int maxStackSize,
                      boolean enabled,
                      long cooldownMillis) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.customModelData = customModelData;
        this.maxStackSize = maxStackSize;
        this.enabled = enabled;
        this.cooldownMillis = cooldownMillis;

        Specialization.getInstance().getCustomItemManager().registerItem(this);

    }

    public abstract void init();



    // === Constructor variations ===
    public CustomItem(String id, String displayName, Material material) {
        this(id, displayName, material, id, -1, true, 0);
    }

    public CustomItem(String id, String displayName, Material material, String customModelData) {
        this(id, displayName, material, customModelData, -1, true, 0);
    }

    public CustomItem(String id, String displayName, Material material, boolean enabled) {
        this(id, displayName, material, id, -1, enabled, 0);
    }

    public CustomItem(String id, String displayName, Material material, long cooldownMillis) {
        this(id, displayName, material, id, -1, true, cooldownMillis);
    }

    public CustomItem(String id, String displayName, Material material, String customModelData, long cooldownMillis) {
        this(id, displayName, material, customModelData, -1, true, cooldownMillis);
    }

    public CustomItem(String id, String displayName, Material material, String customModelData, int maxStackSize, boolean enabled) {
        this(id, displayName, material, customModelData, maxStackSize, enabled, 0);
    }

    // === Accessors ===
    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getMaterial() {
        return material;
    }

    public String getCustomModelData() {
        return customModelData;
    }

    public int getMaxStackSize() {
        return maxStackSize;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public long getCooldownMillis() {
        return cooldownMillis;
    }

    // === Cooldown Logic ===
    public boolean isOnCooldown(Player player) {
        if (cooldownMillis <= 0) return false;
        Long last = lastUse.get(player.getUniqueId());
        return last != null && System.currentTimeMillis() - last < cooldownMillis;
    }

    public void startCooldown(Player player) {
        if (cooldownMillis > 0)
            lastUse.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public long getRemainingCooldown(Player player) {
        if (cooldownMillis <= 0) return 0;
        Long last = lastUse.get(player.getUniqueId());
        if (last == null) return 0;
        long remaining = cooldownMillis - (System.currentTimeMillis() - last);
        return Math.max(remaining, 0);
    }

    public final ItemStack createItemStack(){
        return createItemStack(1, null);
    }

    public final ItemStack createItemStack(int amount){
        return createItemStack(amount, null);
    }

    /**
     * Call this to actually create an ItemStack
     */
    public final ItemStack createItemStack(int amount, Player player) {
        ItemStack item_stack = new ItemStack(material, amount);
        ItemMeta meta = item_stack.getItemMeta();

        meta.getPersistentDataContainer().set(CustomItemManager.key_custom_item_id, PersistentDataType.STRING, getId());

        if(displayName!=null) {
            meta.setDisplayName(displayName);
        }

        if (customModelData != null) {
            CustomModelDataComponent c = meta.getCustomModelDataComponent();
            List<String> strings = new ArrayList<String>();
            strings.add(customModelData);
            c.setStrings(strings);
            meta.setCustomModelDataComponent(c);
        }

        onCreateItem(item_stack, meta);
        item_stack.setItemMeta(meta);

        CustomItemCreationEvent event = new CustomItemCreationEvent(this, item_stack, player);
        Bukkit.getPluginManager().callEvent(event);


        if(event.isCancelled()){
            return null;
        }
        return item_stack;
    };


    /**
     * Extra Logic provided by Custom Item Classes
     */
    public abstract void onCreateItem(ItemStack itemStack, ItemMeta meta);


    /*
     * The Following are Overridable methods.
     * These are called by CustomItemManager's listeners
     * and should remain empty bodies in this class
     */
    public void onInteract(PlayerInteractEvent event, ItemStack itemStack){}
    public void onItemSwitchTo(PlayerItemHeldEvent event, ItemStack newItem) {}
    public void onInteractEntity(PlayerInteractEntityEvent event, ItemStack itemStack){}

    // Called when the player damages a block (e.g., mining)
    public void onBlockBreak(BlockBreakEvent event) {}

    // Called when the player right or left clicks with the item on an entity
//    public void onInteractEntity(ItemStack item_stack, PlayerInteractEntityEvent event, boolean main_hand) {}



    public final void setEnabled(boolean b) {
        this.enabled = b;
//        if(b){
//            CustomItemManager.getInstance().registerItem(this);
//        }else {
//            CustomItemManager.getInstance().unregisterItem(this);
//        }
        Specialization.getInstance().getLogger().info("Custom Item: "+id+ " has been "+ (b?"ENABLED":"DISABLED"));
    }



}
