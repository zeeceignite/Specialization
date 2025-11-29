package com.minecraftcivilizations.specialization.Command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.minecraftcivilizations.specialization.CustomItem.CustomItem;
import com.minecraftcivilizations.specialization.CustomItem.CustomItemManager;
import com.minecraftcivilizations.specialization.CustomItem.EmoteItem;
import com.minecraftcivilizations.specialization.CustomItem.PacketListener;
import com.minecraftcivilizations.specialization.Specialization;
import com.minecraftcivilizations.specialization.util.PlayerUtil;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.ArmorStand;
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
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.*;


/** @author Jfrogy*/

public class EmoteCommand extends BaseCommand implements Listener {

    private final Map<Block, Interaction> seatBlocks = new HashMap<>();
    private final JavaPlugin plugin;
    private final ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

    public EmoteItem clap_item = new EmoteItem("clap_crossbow", "§bClap", EmoteItem.EmoteType.CLAP, "clap", this);
    public EmoteItem point_item = new EmoteItem("point_crossbow", "§bPoint", EmoteItem.EmoteType.POINT, "point", this);
    private final NamespacedKey sitKey = new NamespacedKey(Specialization.getInstance(), "sitemote");

    private final Set<Player> silenced_players = new HashSet<>();

    public EmoteCommand(CustomItemManager customItemManager, JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        protocolManager.addPacketListener(new PacketListener(this));
    }

    public Set<Player> getSilencedPlayers() {
        return silenced_players;
    }

    private void giveEmote(Player player, CustomItem emoteItem, String successMsg) {
        ItemStack hand = player.getInventory().getItemInMainHand();

        if (hand.getType().isAir()) {
            player.getInventory().setItemInMainHand(emoteItem.createItemStack(1, player));
            PlayerUtil.message(player ,successMsg);
            return;
        }

        CustomItem current = CustomItemManager.getInstance().getCustomItem(hand);
        if (current instanceof EmoteItem) {
            player.getInventory().setItemInMainHand(emoteItem.createItemStack(1, player));
            PlayerUtil.message(player ,successMsg);
            return;
        }

        PlayerUtil.message(player ,"Main hand must be empty to emote");
    }

    @CommandAlias("emotes|e")
    @Description("Lists all emote-type custom items")
    public void onList(Player sender) {
        PlayerUtil.message(sender ,"§7==== §eAvailable Emotes §7====");
        PlayerUtil.message(sender ,"§9● §b Sit");
        PlayerUtil.message(sender ,"§9● §b Cannonball");
        PlayerUtil.message(sender ,"§9● §b Fart");
        for (CustomItem item : CustomItemManager.getInstance().getCustomItems()) {
            if (!(item instanceof EmoteItem)) continue;
            boolean enabled = item.isEnabled();
            String icon = enabled ? "§9●" : "§8●";
            PlayerUtil.message(sender ,icon + " §f" + " §7" + item.getDisplayName());
        }
    }

    @CommandAlias("point|p")
    public void givePoint(Player player) {
        if (CustomItemManager.getInstance().getCustomItem("point_crossbow").isEnabled()) {
            giveEmote(player, point_item, "You are now pointing...");
        } else {
            PlayerUtil.message(player ,"§cEmote is disabled");
        }
    }

    @CommandAlias("clap|c")
    public void giveClap(Player player) {
        if (CustomItemManager.getInstance().getCustomItem("clap_crossbow").isEnabled()) {
            giveEmote(player, clap_item, "§9You can now clap... (Tap Right-Click)");
        } else {
            PlayerUtil.message(player ,"§cEmote is disabled");
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

        Block blockUnder = player.getLocation().subtract(0, 1, 0).getBlock();
        Block blockFeet = player.getLocation().getBlock();

        if (!block.equals(blockUnder) && !block.equals(blockFeet)) {
//            player.sendMessage("block: " + blockUnder.getType().name());
            return;
        }
        if (seatBlocks.containsKey(block)) {
            PlayerUtil.message(player ,"§cSomeone is already sitting here.");
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
        if (e.isSneaking()) {
            cancelSeat(e.getPlayer());
            cancelRide(e.getPlayer());
        }
    }


    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        cancelSeat(e.getPlayer());
        cancelRide(e.getPlayer());
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
        cancelRide(event.getPlayer());
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
    private final Map<Player, ArmorStand> sittingStands = new HashMap<>();
    @CommandAlias("sit|s")
    @Description("Sit anywhere using an invisible mini armor stand")
    public void onSit(Player player) {

        if (player.isInsideVehicle()) {
            PlayerUtil.message(player ,"You cant do that right now");
            return;
        }

        Block support = findSolidBlockBelow(player);
        if (support == null) {
            PlayerUtil.message(player ,"No solid block below you");
            return;
        }

        // consistent offset above the block
        double offsetY = 1.02; // adjust as needed

        Location spawnLoc = support.getLocation().add(0.5, offsetY, 0.5);

        // match player facing
        spawnLoc.setYaw(player.getLocation().getYaw());
        spawnLoc.setPitch(0);


        ArmorStand seat = spawnSitStand(player, spawnLoc);

        seat.addPassenger(player);
        sittingStands.put(player, seat);
    }


    private ArmorStand spawnSitStand(Player player, Location loc) {
        World world = player.getWorld();

        // Set facing direction to player
        loc.setYaw(player.getLocation().getYaw());
        loc.setPitch(0f);

        return world.spawn(loc, ArmorStand.class, as -> {
            as.setGravity(true);
            as.setInvulnerable(true);
            as.setPersistent(false);
            as.setVisible(false);
            as.setCollidable(false);
            as.setBasePlate(true);
            as.setSmall(true);
            as.setArms(false);
            as.getAttribute(Attribute.SCALE).setBaseValue(0.01);

            // mark with PDC
            as.getPersistentDataContainer().set(sitKey, PersistentDataType.BYTE, (byte) 1);
        });
    }

    private Block findSolidBlockBelow(Player player) {
        Location loc = player.getLocation();
        World world = loc.getWorld();

        int startY = loc.getBlockY() + 1; // head level
        int minY = Math.max(world.getMinHeight(), startY - 3); // max 3 blocks down

        for (int y = startY; y >= minY; y--) {
            Block b = world.getBlockAt(loc.getBlockX(), y, loc.getBlockZ());
            if (b.getType().isSolid()) {
                return b;
            }
        }

        return null;
    }



    private void cancelRide(Player player) {
        ArmorStand seat = sittingStands.remove(player);

        if (seat != null && seat.isValid()) {
            PersistentDataContainer pdc = seat.getPersistentDataContainer();
            Byte flag = pdc.get(sitKey, PersistentDataType.BYTE);

            if (flag != null && flag == (byte) 1) {
                seat.remove();
            }
        }

        if (player.isInsideVehicle()) {
            player.leaveVehicle();
        }
    }

    @CommandAlias("cannonball|cb")
    @Description("Launch yourself like a cannonball")
    public void onCannonball(Player player) {
        if (player.isInsideVehicle()) {
            PlayerUtil.message(player, "You can't do that right now.");
            return;
        }

        Block support = findSolidBlockBelow(player);
        if (support == null) {
            PlayerUtil.message(player, "No solid block below you.");
            return;
        }

        double offsetY = 1.02;
        Location spawnLoc = support.getLocation().add(0.5, offsetY, 0.5);
        spawnLoc.setYaw(player.getLocation().getYaw());
        spawnLoc.setPitch(0);

        ArmorStand seat = spawnSitStand(player, spawnLoc);
        seat.addPassenger(player);
        sittingStands.put(player, seat);

        // Apply forward and upward velocity
        @NotNull Vector direction = player.getLocation().getDirection().normalize().multiply(0.6); // forward strength
        direction.setY(0.5); // upward strength
        seat.setVelocity(direction);
    }

    @CommandAlias("fart|f")
    @Description("Perform a stinky emote")
    public void onFart(Player player) {

        // REAL sneaking start
        player.setSneaking(true);

        // Fart in 1 second
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            Location loc = player.getLocation().clone().add(0, 0.5, 0);
            loc.add(loc.getDirection().multiply(-0.5));

            player.getWorld().spawnParticle(
                    Particle.DUST,
                    loc,
                    20,
                    0.1, 0.1, 0.1,
                    0,
                    new Particle.DustOptions(Color.fromRGB(0,155,0), 1f)
            );

            // Base pitch
            float basePitch = 0.4f;
            // Variance range (±0.1)
            float variance = 0.2f;

            // Randomized pitch
            float randomPitch = basePitch + (float)((Math.random() * 2 - 1) * variance);

            player.getWorld().playSound(loc, Sound.ENTITY_PIG_AMBIENT, SoundCategory.PLAYERS,0.1f, randomPitch);
            player.getWorld().playSound(loc, Sound.ENTITY_PARROT_IMITATE_PIGLIN, SoundCategory.PLAYERS, 0.5f, randomPitch);

        }, 20L);

        // Uncrouch after 25 ticks total
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.setSneaking(false);
        }, 25L);
    }






}
