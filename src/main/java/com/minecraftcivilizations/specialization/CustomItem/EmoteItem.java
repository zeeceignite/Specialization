package com.minecraftcivilizations.specialization.CustomItem;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.minecraftcivilizations.specialization.Command.EmoteCommand;
import com.minecraftcivilizations.specialization.Specialization;
import com.minecraftcivilizations.specialization.StaffTools.Debug;
import io.papermc.paper.event.entity.EntityLoadCrossbowEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;


public class EmoteItem extends CustomItem implements Listener {

    public enum EmoteType {
        POINT,
        CLAP
    }

    EmoteType emote_type;
    Specialization plugin;
    EmoteCommand emoteCommand;

    public EmoteItem(String id, String display_name, EmoteType type, String model_data, EmoteCommand emoteCommand) {
        super(id, display_name, org.bukkit.Material.CROSSBOW, model_data, -1, true);
        this.emoteCommand = emoteCommand;
        emote_type = type;
        plugin = Specialization.getInstance();
    }




    @Override
    public void onCreateItem(ItemStack itemStack, ItemMeta meta, Player player) {
        if (meta instanceof CrossbowMeta crossbowMeta) {
            if (emote_type == EmoteType.POINT) {
                crossbowMeta.setChargedProjectiles(List.of(new ItemStack(org.bukkit.Material.ARROW)));
                itemStack.setItemMeta(crossbowMeta);
            }
        }
        Debug.broadcast("emote", "<red>added</red> "+player.getName()+" <red>to silenced players");
        emoteCommand.getSilencedPlayers().add(player);
    }

    int arrowSlot = 41;

    @Override
    public void onInteract(PlayerInteractEvent event, ItemStack itemStack) {
        Player player = event.getPlayer();
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        if((emote_type == EmoteType.CLAP)) {
            // Temporary arrow logic
            if (!player.getInventory().contains(org.bukkit.Material.ARROW)) {
                player.getInventory().setItem(arrowSlot, new ItemStack(org.bukkit.Material.ARROW));
            }
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                playClapEffect(player);
            }, 1L);
        }else{
            //POINT
        }
    }

    public void playClapEffect(Player player){
        player.getInventory().setItem(arrowSlot, null);
        if (player.isHandRaised()) {
//            Debug.broadcast("emote", "playing fun sound");
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHERRY_WOOD_PLACE, 0.62f, 0.62f);
//                            playSound(Sound.BLOCK_CHERRY_WOOD_PLACE, 0.29f, 0.29f, 0.75f, 0.75f);
//                            Bukkit.getScheduler().runTaskLater(Specialization.getInstance(), () -> {
//                            //more sounds here
//                        }, 1L);
        }
    }

    public void playSound (Player player, Sound sound, float minVolume, float maxVolume, float minPitch,  float maxPitch){
        player.sendMessage("");
        float volume = minVolume + (float) Math.random() * (maxVolume - minVolume);
        float pitch = minPitch + (float) Math.random() * (maxPitch - minPitch);
        player.playSound(player.getLocation(), sound, SoundCategory.PLAYERS, volume, pitch);

    }

    @Override
    public void onLoadCrossbow(EntityLoadCrossbowEvent event) {
        if (emote_type == EmoteType.CLAP) {
            event.setCancelled(true);
            event.setConsumeItem(false);
        }
    }

    @Override
    public void onShootBow(EntityShootBowEvent event) {
        if (emote_type == EmoteType.POINT) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onPlayerDeath(PlayerDeathEvent event, ItemStack item_stack) {
        item_stack.setAmount(0);
    }

    @Override
    public void onItemSwitchAway(PlayerItemHeldEvent event, ItemStack custom_item_stack, ItemStack newItem) {
        event.getPlayer().getInventory().remove(custom_item_stack);
        if (emote_type == EmoteType.CLAP) {
            event.getPlayer().sendMessage(MiniMessage.miniMessage().deserialize("<gray>Clap ability removed..."));
        } else {
            event.getPlayer().sendMessage(MiniMessage.miniMessage().deserialize("<gray>You stop pointing..."));
        }
        Debug.broadcast("emote", "<red>removed</red> "+event.getPlayer().getName());
        emoteCommand.getSilencedPlayers().remove(event.getPlayer());
    }

    @Override
    public void onInventoryClick(InventoryClickEvent event, ItemStack itemStack) {
        event.setCancelled(true);
    }


    @Override
    public void onDropItemByPlayer(PlayerDropItemEvent event) {
        event.setCancelled(true);
    }
}
