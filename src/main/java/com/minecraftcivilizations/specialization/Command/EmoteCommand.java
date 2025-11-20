package com.minecraftcivilizations.specialization.Command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.minecraftcivilizations.specialization.CustomItem.CustomItem;
import com.minecraftcivilizations.specialization.CustomItem.CustomItemManager;
import com.minecraftcivilizations.specialization.CustomItem.EmoteItem;
import com.minecraftcivilizations.specialization.CustomItem.EmotePacketListener;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

@CommandPermission("specialization.emote")
public class EmoteCommand extends BaseCommand implements Listener {

    private final Map<Block, Interaction> seatBlocks = new HashMap<>();
    private final JavaPlugin plugin;
    private final ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

    public EmoteItem clap_item = new EmoteItem("clap_crossbow", "§bClap", EmoteItem.EmoteType.CLAP, "clap", this);
    public EmoteItem point_item = new EmoteItem("point_crossbow", "§bPoint", EmoteItem.EmoteType.POINT, "point", this);

    private final Set<Player> silenced_players = new HashSet<>();

    public EmoteCommand(CustomItemManager customItemManager, JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        protocolManager.addPacketListener(new EmotePacketListener(this));
    }

    public Set<Player> getSilencedPlayers() {
        return silenced_players;
    }

    private void giveEmote(Player player, CustomItem emoteItem, String successMsg) {
        ItemStack hand = player.getInventory().getItemInMainHand();

        if (hand.getType().isAir()) {
            player.getInventory().setItemInMainHand(emoteItem.createItemStack(1, player));
            player.sendMessage(successMsg);
            return;
        }

        CustomItem current = CustomItemManager.getInstance().getCustomItem(hand);
        if (current instanceof EmoteItem) {
            player.getInventory().setItemInMainHand(emoteItem.createItemStack(1, player));
            player.sendMessage(successMsg);
            return;
        }

        player.sendMessage("§cMain hand must be empty to emote");
    }

    @CommandAlias("emotes|e")
    @Description("Lists all emote-type custom items")
    @CommandPermission("civlabs.emotes")
    public void onList(Player sender) {
        sender.sendMessage("§7==== §eAvailable Emotes §7====");
        for (CustomItem item : CustomItemManager.getInstance().getCustomItems()) {
            if (!(item instanceof EmoteItem)) continue;
            boolean enabled = item.isEnabled();
            String icon = enabled ? "§9●" : "§8●";
            sender.sendMessage(icon + " §f" + " §7" + item.getDisplayName());
        }
    }

    @CommandAlias("point|p")
    public void givePoint(Player player) {
        if (CustomItemManager.getInstance().getCustomItem("point_crossbow").isEnabled()) {
            giveEmote(player, point_item, "§9You are now pointing...");
        } else {
            player.sendMessage("§cEmote is disabled");
        }
    }

    @CommandAlias("clap|c")
    public void giveClap(Player player) {
        if (CustomItemManager.getInstance().getCustomItem("clap_crossbow").isEnabled()) {
            giveEmote(player, clap_item, "§9You can now clap... (Tap Right Click)");
        } else {
            player.sendMessage("§cEmote is disabled");
        }
    }

    // --- Right-click sit logic ---
    @EventHandler
    public void onPlayerRightClick(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (!isValidSeatBlock(block)) return;

        Player player = event.getPlayer();
        if (!player.getInventory().getItemInMainHand().getType().isAir()) return;
        if (player.isSneaking() || isPlayerSitting(player)) return;
        if (block.getLocation().distanceSquared(player.getLocation()) > 2.25) return;
        if (seatBlocks.containsKey(block)) {
            player.sendMessage("§cSomeone is already sitting here.");
            return;
        }
        sit(player, block);
        event.setCancelled(true);
    }

    private void sit(Player player, Block block) {
        Location loc = getSeatLocation(block);
        Interaction seat = block.getWorld().spawn(loc, Interaction.class, i -> {
            i.setInteractionWidth(0.6f);
            i.setInteractionHeight(0f);
            i.setResponsive(false);
            i.setInvulnerable(true);
            i.setGravity(false);
        });

        seatBlocks.put(block, seat);
        if (seat.isValid() && seat.getPassengers().isEmpty() && !player.isInsideVehicle()) {
            seat.addPassenger(player);
        }
    }

    private boolean isPlayerSitting(Player player) {
        for (Interaction seat : seatBlocks.values()) {
            if (seat.getPassengers().contains(player)) return true;
        }
        return false;
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        if (e.isSneaking()) cancelSeat(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        cancelSeat(e.getPlayer());
    }

    @EventHandler
    public void onBlockPhysics(BlockPhysicsEvent e) {
        Block changed = e.getBlock();

        // Ignore if this block isn't being used as a seat
        if (!seatBlocks.containsKey(changed)) return;

        // If the seat block itself has turned into something non-solid (e.g. broken)
        if (!changed.getType().isSolid()) {
            Interaction seat = seatBlocks.remove(changed);
            if (seat != null && seat.isValid()) seat.remove();
        }
    }

    private void cancelSeat(Player player) {
        Block toRemove = null;
        for (Map.Entry<Block, Interaction> entry : seatBlocks.entrySet()) {
            Interaction seat = entry.getValue();
            if (seat.getPassengers().contains(player)) {
                toRemove = entry.getKey();
                seat.remove();
                break;
            }
        }
        if (toRemove != null) seatBlocks.remove(toRemove);
        if (player.isInsideVehicle()) player.leaveVehicle();
    }

    private boolean isValidSeatBlock(Block block) {
        if (block == null) return false;
        Block above = block.getRelative(BlockFace.UP);
        if (!above.isPassable()) return false; // player must have space above

        Material type = block.getType();
        String name = type.name();

        if (name.endsWith("_STAIRS")) {
            Stairs stairs = (Stairs) block.getBlockData();
            return stairs.getHalf() == Stairs.Half.BOTTOM;
        }

        if (name.endsWith("_SLAB")) {
            return true;
        }

        // Carpet support
        if (name.endsWith("_CARPET")) {
            return true;
        }

        return false;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        cancelSeat(event.getPlayer());
        if (event.getPlayer().isInsideVehicle()) event.getPlayer().leaveVehicle();
    }
    private Location getSeatLocation(Block block) {
        Location loc = block.getLocation().clone().add(0.5, 0, 0.5);
        String name = block.getType().name();

        double yOffset = 0;
        double xOffset = 0;
        double zOffset = 0;

        if (name.endsWith("_STAIRS")) {
            yOffset = 0.55;

            if (block.getBlockData() instanceof org.bukkit.block.data.Directional dir) {
                switch (dir.getFacing()) {
                    case NORTH -> zOffset = 0.02;
                    case SOUTH -> zOffset = -0.02;
                    case WEST  -> xOffset = 0.02;
                    case EAST  -> xOffset = -0.02;
                }

                loc.setYaw(switch (dir.getFacing()) {
                    case NORTH -> 180f;
                    case SOUTH -> 0f;
                    case WEST  -> 90f;
                    case EAST  -> -90f;
                    default -> 0f;
                });
            }

        } else if (name.endsWith("_SLAB")) {
            yOffset = 0.55;


        } else if (name.endsWith("_CARPET")) {
            // Carpet is not directional
            yOffset = 0.03;
        }

        loc.add(xOffset, yOffset, zOffset);
        return loc;
    }


}
