package com.minecraftcivilizations.specialization.Listener.Player.Inventories;

import com.google.gson.reflect.TypeToken;
import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillLevel;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.StaffTools.Debug;
import minecraftcivilizations.com.minecraftCivilizationsCore.MinecraftCivilizationsCore;
import minecraftcivilizations.com.minecraftCivilizationsCore.Options.Pair;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.Recipe;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class CraftingListener implements Listener {

    private static final Logger LOGGER = Logger.getLogger(CraftingListener.class.getName());
    private final Plugin plugin;

    private static final Set<Material> COMPLEX_ITEMS = Set.of(
            Material.DIAMOND_PICKAXE, Material.DIAMOND_AXE, Material.DIAMOND_SHOVEL, Material.DIAMOND_HOE, Material.DIAMOND_SWORD,
            Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS,
            Material.NETHERITE_PICKAXE, Material.NETHERITE_AXE, Material.NETHERITE_SHOVEL, Material.NETHERITE_HOE, Material.NETHERITE_SWORD,
            Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS,
            Material.ANVIL,
            Material.SMITHING_TABLE,
            Material.BLAST_FURNACE,
            Material.GRINDSTONE,

            Material.PISTON, Material.STICKY_PISTON,
            Material.DISPENSER, Material.DROPPER,
            Material.OBSERVER,
            Material.HOPPER,
            Material.COMPARATOR,
            Material.REPEATER,
            Material.DAYLIGHT_DETECTOR,
            Material.SCAFFOLDING,
            Material.JUKEBOX,
            Material.CAMPFIRE,

            Material.ENCHANTING_TABLE,
            Material.BOOKSHELF,
            Material.LECTERN,

            Material.BREWING_STAND,
            Material.GLISTERING_MELON_SLICE,
            Material.GOLDEN_CARROT,
            Material.GOLDEN_APPLE,


            Material.BEACON,
            Material.ENDER_CHEST,
            Material.SHIELD,
            Material.CROSSBOW,
            Material.TNT,
            Material.TARGET,

            Material.CAKE,
            Material.PUMPKIN_PIE,
            Material.RABBIT_STEW
    );

    public CraftingListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || event.getCurrentItem() == null) return;

        if (!isCraftingActionValid(event)) {
            event.setResult(Event.Result.DENY);
            event.setCancelled(true);
            return;
        }

        ItemStack crafted = event.getCurrentItem();

        if (COMPLEX_ITEMS.contains(crafted.getType())) {
            CustomPlayer customPlayer = (CustomPlayer) MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().getCustomPlayer(player.getUniqueId());

            int amount = getCraftedAmount(event);
            for(int i = 0; i < amount; i++) {
                customPlayer.getAnalyticPlayerData().incrementComplexItemsCrafted(crafted.getType().toString());
            }
            Debug.broadcast("analytics", player.getName() + " crafted complex item: " + crafted.getType() + " x" + amount);
        }

        Pair<SkillType, Double> xp_gain_pair = SpecializationConfig.getXpGainFromCraftingConfig()
                .get(crafted.getType(), new TypeToken<>() {});

        int craftedAmount = getCraftedAmount(event);

        String amtstring = "<gold>x"+craftedAmount+"</gold>";
        if(craftedAmount==1)amtstring = "";

        Debug.broadcast("craft",
                "<gray>🎬:</gray> "+event.getAction().name() + " "+amtstring+" <blue>📦: "+event.getCurrentItem().getType().name()+"</blue> <green>🖱:"+event.getCursor().getType().name(),
                "<blue>Current Item: </blue>"+event.getCurrentItem().getType().name()+"\n"
                +"<green>Cursor Item: </green>"+event.getCursor().getType().name());




        CustomPlayer customPlayer = (CustomPlayer) MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().getCustomPlayer(player.getUniqueId());


        int lvl = customPlayer.getSkillLevel(xp_gain_pair.firstValue());
        double xpGainBenefit = (5-((double)lvl)/1.5);
        // Reduction based on Skill Level and Amount Crafted
        int totalReduction = (int) (xpGainBenefit *  (craftedAmount));
        totalReduction = Math.max(1, totalReduction);//Math.max(0, totalReduction - (int) (Math.random() * 3));

        int foodLevel = player.getFoodLevel();
        if(player.getGameMode()==GameMode.CREATIVE){
            foodLevel=220;
        }

        Debug.broadcast("craft", "<red>Food Level: </red>"+foodLevel+" <gold>Reduction:</gold> "+totalReduction);

        if(foodLevel < totalReduction || foodLevel < 1){
            event.setResult(Event.Result.DENY);
            event.setCancelled(true);
            player.playSound(player.getLocation(), Sound.BLOCK_CHORUS_FLOWER_GROW, 0.5f, 1.25f);
            if(craftedAmount>1){
                player.sendActionBar(MiniMessage.miniMessage().deserialize("<red>You're too hungry to craft that many</red>"));
            }else{
                player.sendActionBar(MiniMessage.miniMessage().deserialize("<red>You're too hungry to craft</red>"));
            }
            return;
        }


        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            ItemStack testItem = crafted.clone();
            testItem.setAmount(craftedAmount * crafted.getAmount());
            if (!canFitInInventory(player, testItem)) {
                event.setCancelled(true);
                event.setResult(Event.Result.DENY);
                return;
            }
        }


        SpecializationCraftItemEvent new_event = new SpecializationCraftItemEvent(event, player, craftedAmount, totalReduction, xp_gain_pair.firstValue(), lvl);
        Bukkit.getPluginManager().callEvent(new_event);
        if(new_event.isXpCancelled()) {
            return;
        }
        if (xp_gain_pair.firstValue() != null && xp_gain_pair.secondValue() != null) {
            double xpToGive = xp_gain_pair.secondValue() * craftedAmount;

            int finalReduction = Math.max(totalReduction, 1);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.setFoodLevel(player.getFoodLevel() - finalReduction);
                    customPlayer.addSkillXp(xp_gain_pair.firstValue(), xpToGive);
                    LOGGER.fine("Gave " + xpToGive + " XP to " + player.getName() +
                            " for crafting " + craftedAmount + "x " + crafted.getType());
                }
            }, 1L);
        }

    }

    /**
     * Checks if the player's inventory has space for the given item stack
     */
    private boolean canFitInInventory(Player player, ItemStack item) {
        int amountToAdd = item.getAmount();
        int maxStackSize = item.getMaxStackSize();

        for (ItemStack invItem : player.getInventory().getStorageContents()) {
            if (amountToAdd <= 0) break;

            if (invItem == null || invItem.getType().isAir()) {
                // Empty slot can fit a full stack
                amountToAdd -= maxStackSize;
            } else if (invItem.isSimilar(item)) {
                // Existing stack can fit more
                int spaceLeft = maxStackSize - invItem.getAmount();
                amountToAdd -= spaceLeft;
            }
        }

        return amountToAdd <= 0;
    }

    /**
     * Determines if the crafting action will actually consume ingredients
     * and produce items in the player's inventory.
     */
    private boolean isCraftingActionValid(CraftItemEvent event) {
        InventoryAction action = event.getAction();
        if(action==InventoryAction.MOVE_TO_OTHER_INVENTORY)return true;

        return switch (action) {
            case PICKUP_ALL, PICKUP_SOME, PICKUP_HALF, PICKUP_ONE, PLACE_ALL, PLACE_SOME,
                 PLACE_ONE, SWAP_WITH_CURSOR, HOTBAR_SWAP, DROP_ALL_CURSOR, DROP_ALL_SLOT, DROP_ONE_CURSOR -> true;
            case DROP_ONE_SLOT -> (event.getCursor().getType().isAir());
                 default -> false;
        };
    }

    /**
     * Calculates how many items are actually being crafted based on the event action
     */
    private int getCraftedAmount(CraftItemEvent event) {
        ItemStack result = event.getCurrentItem();
        if (result == null) return 0;
        if(event.getResult().equals(Event.Result.DENY)){
            return 0;
        }

        InventoryAction action = event.getAction();

        switch (action) {
            case PICKUP_HALF:
                return Math.max(1, result.getAmount() / 2);
            case PICKUP_SOME:
                ItemStack cursor = event.getCursor();
                if (cursor.isSimilar(result)) {
                    int maxStack = result.getMaxStackSize();
                    int canTake = maxStack - cursor.getAmount();
                    return Math.min(canTake, result.getAmount());
                }
                return result.getAmount();
            case MOVE_TO_OTHER_INVENTORY:
                return calculateBulkCraftAmount(event);
            case DROP_ONE_CURSOR, DROP_ALL_SLOT, DROP_ALL_CURSOR, DROP_ONE_SLOT:
                if(!event.getWhoClicked().getItemOnCursor().getType().equals(Material.AIR)) return 0;
            default:
                return result.getAmount();
        }
    }

    /**
     * Calculates the actual number of crafting operations for bulk crafting (Shift+Click)
     */
    private int calculateBulkCraftAmount(CraftItemEvent event) {
        ItemStack result = event.getCurrentItem();
        if (result == null) return 0;

        // Get the recipe and check ingredient availability
        Recipe recipe = event.getRecipe();
        if (recipe == null) return 0;

        // For bulk crafting, we need to determine how many times the recipe can be executed
        // based on available ingredients in the crafting matrix
        org.bukkit.inventory.CraftingInventory craftingInventory = event.getInventory();
        ItemStack[] matrix = craftingInventory.getMatrix();
        
        int maxCrafts = Integer.MAX_VALUE;
        
        // Check each ingredient slot to find the limiting factor
        for (ItemStack ingredient : matrix) {
            if (ingredient != null && ingredient.getAmount() > 0) {
                // Each crafting operation consumes 1 of this ingredient
                maxCrafts = Math.min(maxCrafts, ingredient.getAmount());
            }
        }
        
        // If no ingredients found or unlimited, default to result amount divided by recipe yield
        if (maxCrafts == Integer.MAX_VALUE) {
            return result.getAmount();
        }
        
        return maxCrafts * event.getRecipe().getResult().getAmount();
    }/**
     * Returns the exact ItemStacks that will be added to the player's inventory
     * when doing a bulk craft (shift+click / MOVE_TO_OTHER_INVENTORY).
     * The returned stacks are clones (safe to mutate).
     */
    private List<ItemStack> getStacksAddedByBulkCraft(Player player, ItemStack result, int totalProduced) {
        List<ItemStack> added = new ArrayList<>();
        if (result == null || totalProduced <= 0) return added;

        PlayerInventory inv = player.getInventory();
        int maxStack = result.getMaxStackSize();
        int remaining = totalProduced;

        // First try to fill existing similar stacks
        for (int i = 0; i < inv.getSize() && remaining > 0; i++) {
            ItemStack slot = inv.getItem(i);
            if (slot == null || slot.getType().isAir()) continue;
            if (!slot.isSimilar(result)) continue;

            int space = maxStack - slot.getAmount();
            if (space <= 0) continue;

            int toAdd = Math.min(space, remaining);
            ItemStack addedStack = result.clone();
            addedStack.setAmount(toAdd);
            added.add(addedStack);
            remaining -= toAdd;
        }

        // Then fill empty slots
        for (int i = 0; i < inv.getSize() && remaining > 0; i++) {
            ItemStack slot = inv.getItem(i);
            if (slot != null && !slot.getType().isAir()) continue;

            int toAdd = Math.min(maxStack, remaining);
            ItemStack addedStack = result.clone();
            addedStack.setAmount(toAdd);
            added.add(addedStack);
            remaining -= toAdd;
        }

        // remaining > 0 means not all produced items fit; those remain in grid.
        return added;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        if (event.getInventory().getViewers().isEmpty()) return;

        Player player = null;

        for (var viewer : event.getInventory().getViewers()) {
            if (viewer instanceof Player) {
                player = (Player) viewer;
                break;
            }
        }

        if (player == null) return;

        Recipe recipe = event.getRecipe();
        if (recipe == null) return;

        if (!(recipe instanceof Keyed)) {
            return;
        }

        NamespacedKey recipeKey = ((Keyed) recipe).getKey();

        if (shouldBlockRecipe(player, recipeKey)) {
            LOGGER.info("Blocking recipe " + recipeKey + " for player " + player.getName() + " due to insufficient skill level");
            event.getInventory().setResult(null);
            player.undiscoverRecipe(recipeKey);
        } else {
            if (!player.hasDiscoveredRecipe(recipeKey)) {
                player.discoverRecipe(recipeKey);
                LOGGER.fine("Discovered recipe " + recipeKey + " for player " + player.getName() + " on-demand");
            }
        }
    }

    public static boolean shouldBlockRecipe(Player player, NamespacedKey recipeKey) {
        CustomPlayer customPlayer = (CustomPlayer) MinecraftCivilizationsCore.getInstance()
                .getCustomPlayerManager().getCustomPlayer(player.getUniqueId());
        if(customPlayer.getAdditionUnlockedRecipes() != null && customPlayer.getAdditionUnlockedRecipes().contains(recipeKey)) return false;

        // Check if recipe is in any skill-specific unlocked recipes config
        for (SkillType skillType : SkillType.values()) {
            for (SkillLevel skillLevel : SkillLevel.values()) {
                String configKey = skillType + "_" + skillLevel;
                Set<NamespacedKey> skillRecipes = SpecializationConfig.getUnlockedRecipesConfig()
                        .get(configKey, new TypeToken<>() {
                        });

                if (skillRecipes != null && skillRecipes.contains(recipeKey)) {
                    return customPlayer.getSkillLevel(skillType) < skillLevel.ordinal();
                }
            }
        }

        // Recipe is not in any skill config - allow it
        return false;
    }
}