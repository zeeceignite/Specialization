package com.minecraftcivilizations.specialization.Listener.Player;

import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Specialization;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;

public class LocalChat implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        event.setCancelled(true);

        Player sender = event.getPlayer();
        String originalMessage = MiniMessage.miniMessage().stripTags(event.getMessage().trim());
        if(tryHandleGlobalChat(sender, originalMessage)) return;

        String defaultFormat = SpecializationConfig.getChatConfig().get("DEFAULT_FORMAT", String.class);
        Bukkit.getScheduler().runTask(Specialization.getInstance(), () -> {
            getNearbyPlayers(sender).forEach(player -> {
                player.sendRichMessage(defaultFormat.formatted(sender.getName(), originalMessage)); //TODO: replace with anon name
            });
        });
        sender.sendRichMessage(defaultFormat.formatted(sender.getName(), originalMessage));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e){
        e.joinMessage(null);
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e){
        e.quitMessage(null);
    }

    private boolean tryHandleGlobalChat(Player player, String message) {
        String prefix = SpecializationConfig.getChatConfig().get("ANNOUNCEMENT_PREFIX", String.class);
        if(!message.startsWith(prefix) || !player.isOp()) return false;
        String actualMessage = message.substring(prefix.length()).trim();
        String announcementFormat = SpecializationConfig.getChatConfig().get("ANNOUNCEMENT_FORMAT", String.class);
        Bukkit.getOnlinePlayers().forEach(other -> other.sendRichMessage(announcementFormat.formatted(actualMessage)));
        return true;
    }

    private List<Player> getNearbyPlayers(Player player) {
        double radius = SpecializationConfig.getChatConfig().get("CHAT_RADIUS", Double.class);
        return player.getNearbyEntities(radius,radius,radius).stream().filter(entity -> entity instanceof Player)
                .map(entity -> (Player) entity).toList();
    }
}
