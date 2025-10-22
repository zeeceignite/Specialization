package com.minecraftcivilizations.specialization.Debug;

import com.minecraftcivilizations.specialization.Specialization;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Utilies that aim to help with server development and debugging
 * Please use it when trying to debug things.
 * see: DebugListenCommand.java for commands
 * @author Alectriciti
 */
public class Debug {

    /**
     * intended use:
     * /debug add <debug_channel>
     * /debug remove <debug_channel>
     */

    // debug_channel -> List of Players registered to that channel
    private final Map<String, Set<Player>> debug_listening = new HashMap<String, Set<Player>>();
    private final Map<Player, List<String>> listening_channels = new HashMap<Player, List<String>>(); //used specifically for tab completion
    private final List<String> debug_channels = new ArrayList<String>(); //used by command suggestions

    /**
     * This registers a player to a debug channel to listen to it
     */
    public void registerPlayerChannel(Player player, String debug_channel){
        if (!player.hasPermission("specialization.debug")) {
            return;
        }
        Set<Player> player_set = getOrCreatePlayerSet(debug_channel, false);
        player_set.add(player);
    }

    /**
     * This unregisters a player fromm the channel they're listening to
     */
    public void unregisterPlayerChannel(Player player, String debug_channel){
        Set<Player> player_set = getOrCreatePlayerSet(debug_channel, false);
        player_set.remove(player);
    }


    private Set<Player> getOrCreatePlayerSet(String debug_channel, boolean add_to_suggestions){
        Set<Player> player_set;
        //retrieve debug channel list
        if(debug_listening.containsKey(debug_channel)) {
            player_set = debug_listening.get(debug_channel);
            if(add_to_suggestions && !debug_channels.contains(debug_channel)) debug_channels.add(debug_channel); //add for command-suggest quick lookup
        }else{
            player_set = new HashSet<Player>();
            debug_listening.put(debug_channel, player_set); //add for registry
            if(add_to_suggestions) debug_channels.add(debug_channel); //add for command-suggest quick lookup
        }
        return player_set;
    }

    /**
     * Sends a debug broadcast to ALL players within a specific debug channnel
     * /debug add <debug_channel>
     * /debug remove <debug_channel>
     */
    public static void broadcast(String debug_channel, String msg, String hover_event){
        Component comp = getInstance().formatDebugMessageDefault(debug_channel, msg, hover_event);
        broadcastFinalize(debug_channel, comp, false);
    }

    /**
     *
     * @param debug_channel The channel to send this debug message to
     * @param msg The base debug message
     * @param hover_event A sub message to hover for the debug message
     * @param register_channel whether or not this channel gets added to the TabCompleter for the command
     */
    public static void broadcast(String debug_channel, String msg, String hover_event, boolean register_channel){
        Component comp = getInstance().formatDebugMessageDefault(debug_channel, msg, hover_event);
        broadcastFinalize(debug_channel, comp, register_channel);
    }

    public static void broadcast(String debug_channel, Component msg, Component hover){
        Component comp;
        if(hover!=null){
            comp = msg.hoverEvent(HoverEvent.showText(hover));
        }else{
            comp = msg;
        }
        broadcastFinalize(debug_channel, getPrefix(debug_channel).append(comp), false);
    }
    public static void broadcast(String debug_channel, Component msg, Component hover, boolean register_channel){
        Component comp = msg.hoverEvent(HoverEvent.showText(hover));
        broadcastFinalize(debug_channel, getPrefix(debug_channel).append(comp), register_channel);
    }

    private static void broadcastFinalize(String debug_channel, Component comp, boolean register_channel) {
        for(Player player : getInstance().getOrCreatePlayerSet(debug_channel, register_channel)){
            player.sendMessage(comp);
        }
    }

    public static void broadcast(String debug_channel, String msg){
        broadcast(debug_channel, msg, null);
    }


    public static void message(Player player, String debug_channel, String msg){
        message(player, debug_channel, msg, null);
    }
    /**
     * Sends a debug to a player who is listening to a debug channel
     * /debug add <debug_channel>
     * /debug remove <debug_channel>
     */
    public static void message(Player player, String debug_channel, String msg, String hover_details){
        Debug debug = getInstance();
        Component comp = debug.formatDebugMessageDefault(debug_channel, msg, hover_details);
        if(debug.getOrCreatePlayerSet(debug_channel, false).contains(player)){
            player.sendMessage(comp);
        }
    }

    /**
     * The Default Formatting for using Strings in debugMessage or debugBroadcast
     * @param debug_channel
     * @param msg
     * @param hover_details
     * @return
     */
    Component formatDebugMessageDefault(String debug_channel, String msg, String hover_details){
        Component comp = getPrefix(debug_channel).append(Component.text(msg));//+msg);
        if(hover_details!=null) {
            Component hover = MiniMessage.miniMessage().deserialize(hover_details);
            return comp.hoverEvent(HoverEvent.showText(hover));
        }else{
            return comp;
        }
//        return comp;
    }

    private static @NotNull Component getPrefix(String debug_channel) {
        return MiniMessage.miniMessage().deserialize("<dark_gray>[debug:" + debug_channel + "]:</dark_gray> ");
    }

    /**
     * Used for Tab Completion with the command
     */
    public static List<String> getChannelList(){
        return getInstance().debug_channels;
    }

    public static List<String> getPlayerChannels(Player player){
        Debug debug = getInstance();
        if(debug.listening_channels.containsKey(player)){
            return debug.listening_channels.get(player);
        }
        return new ArrayList<String>();
    }

    public static Debug getInstance(){
        return Specialization.getInstance().getDebugUtils();
    }

}