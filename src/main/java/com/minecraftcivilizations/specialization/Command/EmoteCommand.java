package com.minecraftcivilizations.specialization.Command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.minecraftcivilizations.specialization.CustomItem.CustomItem;
import com.minecraftcivilizations.specialization.CustomItem.CustomItemManager;
import com.minecraftcivilizations.specialization.CustomItem.EmoteItem;
import com.minecraftcivilizations.specialization.CustomItem.EmotePacketListener;
import com.minecraftcivilizations.specialization.StaffTools.Debug;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

@CommandAlias("emote|e")
@CommandPermission("specialization.emote")
public class EmoteCommand extends BaseCommand implements Listener {

    private final Map<Player, Interaction> activeBase = new HashMap<>();
    private final Map<Player, BukkitTask> activePoint = new HashMap<>();
    private final JavaPlugin plugin;
//    private final EmoteItem emotes;

    ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

    // --- NEW: Clap Crossbow ---
    public EmoteItem clap_item = new EmoteItem("clap_crossbow", "§bClap", EmoteItem.EmoteType.CLAP, "clap", this);
    // --- NEW: Point Crossbow ---
    public EmoteItem point_item = new EmoteItem("point_crossbow", "§6Point", EmoteItem.EmoteType.POINT, "point", this);

    public EmoteCommand(CustomItemManager customItemManager, JavaPlugin plugin) {
        this.plugin = plugin;


        Bukkit.getPluginManager().registerEvents(this, plugin);
        protocolManager.addPacketListener(new EmotePacketListener(this));
    }

    Set<Player> silenced_players = new HashSet<>();

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

    @Subcommand("point|p")
    public void givePoint(Player player) {
        giveEmote(player, point_item, "§9You are now pointing");
    }

    @Subcommand("clap|c")
    public void giveClap(Player player) {
        giveEmote(player, clap_item, "§9You can now clap");
    }



    // --- Right-click sit logic ---
    @EventHandler
    public void onPlayerRightClick(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (!isValidSeatBlock(block)) return;

        Player player = event.getPlayer();
        if (!player.getInventory().getItemInMainHand().getType().isAir()) return;
        if (player.isSneaking() || activeBase.containsKey(player)) return;
        if (block.getLocation().distanceSquared(player.getLocation()) > 2.25) return;
        if (blockHasPassenger(block)) {
            player.sendMessage("§cSomeone is already sitting here.");
            return;
        }

        sit(player, block);
        event.setCancelled(true);
    }

    private void sit(Player player, Block block) {
        Location loc = getSeatLocation(block);

        Interaction seat = block.getWorld().spawn(loc, Interaction.class, i -> {
            i.setResponsive(false);
            i.setInteractionWidth(0);
            i.setInteractionHeight(0);
            i.setInvulnerable(true);
            i.setGravity(false);
        });

        activeBase.put(player, seat);
        if (seat.isValid() && !player.isInsideVehicle() && seat.getPassengers().isEmpty()) {
            seat.addPassenger(player);
        }
    }

    private boolean blockHasPassenger(Block block) {
        Location loc = getSeatLocation(block);
        for (Interaction seat : activeBase.values()) {
            if (seat.getLocation().distanceSquared(loc) < 0.01) return true;
        }
        return false;
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();
        if (e.isSneaking()) cancelEmote(p);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        cancelEmote(e.getPlayer());
        BukkitTask t = activePoint.remove(e.getPlayer());
        if (t != null) t.cancel();
    }

    @EventHandler
    public void onBlockPhysics(BlockPhysicsEvent e) {
        activeBase.forEach((player, seat) -> {
            Block below = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
            if (seat.getLocation().getBlock().equals(e.getBlock()) || below.getType().isAir()) cancelEmote(player);
        });
    }

    private void cancelEmote(Player player) {
        Interaction seat = activeBase.remove(player);
        if (seat != null && !seat.isDead()) seat.remove();
        if (player.isInsideVehicle()) player.leaveVehicle();
    }

    private boolean isValidSeatBlock(Block block) {
        if (block == null) return false;
        Block above = block.getRelative(BlockFace.UP);
        if (!above.isPassable()) return false;

        Material type = block.getType();
        String name = type.name();

        if (name.endsWith("_STAIRS")) {
            Stairs stairs = (Stairs) block.getBlockData();
            return stairs.getHalf() == Stairs.Half.BOTTOM;
        }
        if (name.endsWith("_SLAB")) return true;
        return type.isSolid();
    }

    private Location getSeatLocation(Block block) {
        Location loc = block.getLocation().clone().add(0.5, 0, 0.5);
        Material type = block.getType();
        String name = type.name();

        if (name.endsWith("_STAIRS") || name.endsWith("_SLAB")) loc.add(0, 0.5, 0);

        if (block.getBlockData() instanceof org.bukkit.block.data.Directional dir) {
            switch (dir.getFacing()) {
                case NORTH -> loc.add(0, 0, 0.25);
                case SOUTH -> loc.add(0, 0, -0.25);
                case WEST -> loc.add(0.25, 0, 0);
                case EAST -> loc.add(-0.25, 0, 0);
            }
            loc.setYaw(switch (dir.getFacing()) {
                case NORTH -> 0f;
                case SOUTH -> 180f;
                case WEST -> -90f;
                case EAST -> 90f;
                default -> 0f;
            });
        }
        return loc;
    }
}
