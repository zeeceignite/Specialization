package com.minecraftcivilizations.specialization.Listener;

import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Specialization;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.List;

public class LocalChat implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        event.setCancelled(true);

        Player sender = event.getPlayer();
        String originalMessage = MiniMessage.miniMessage().stripTags(event.getMessage().trim());
        if(tryHandleGlobalChat(sender, originalMessage)) return;

        String defaultFormat = SpecializationConfig.getChatConfig().getString("DEFAULT_FORMAT");
        Bukkit.getScheduler().runTask(Specialization.getInstance(), () -> {
            getNearbyPlayers(sender).forEach(player -> {
                player.sendRichMessage(defaultFormat.formatted(sender.getName(), originalMessage)); //TODO: replace with anon name
            });
        });
        sender.sendRichMessage(defaultFormat.formatted(sender.getName(), originalMessage));
        Specialization.logger.info("<" + sender + "> " + originalMessage);
    }

    private boolean tryHandleGlobalChat(Player player, String message) {
        String prefix = SpecializationConfig.getChatConfig().getString("ANNOUNCEMENT_PREFIX");
        if(!message.startsWith(prefix) || !player.isOp()) return false;
        String actualMessage = message.substring(prefix.length());
        String announcementFormat = SpecializationConfig.getChatConfig().getString("ANNOUNCEMENT_FORMAT");
        Bukkit.getOnlinePlayers().forEach(other -> other.sendRichMessage(announcementFormat.formatted(actualMessage)));
        return true;
    }

    private List<Player> getNearbyPlayers(Player player) {
        double radius = SpecializationConfig.getChatConfig().getDouble("CHAT_RADIUS");
        return player.getNearbyEntities(radius,radius,radius).stream().filter(entity -> entity instanceof Player)
                .map(entity -> (Player) entity).toList();
    }
}
