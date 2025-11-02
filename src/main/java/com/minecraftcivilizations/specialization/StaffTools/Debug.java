package com.minecraftcivilizations.specialization.StaffTools;

import com.minecraftcivilizations.specialization.Specialization;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.*;

/**
 * Utilies that aim to help with server development and debugging
 * Please use it when trying to debug things.
 * see: DebugListenCommand.java for commands
 */
public class Debug implements Listener {

    public static String TITLE = ChatColor.DARK_GRAY + "[debug]";
    /**
     * intended use:
     * /debug add <debug_channel>
     * /debug remove <debug_channel>
     */

    // debug_channel -> List of Players registered to that channel
    private Map<String, Set<Player>> debug_listening = new HashMap<String, Set<Player>>();
    private Map<UUID, List<String>> listening_channels = new HashMap<UUID, List<String>>(); //used specifically for tab completion
    private List<String> debug_channels = new ArrayList<String>(); //used by command suggestions


    public Debug(Specialization plugin){
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        setupDefaultChannels();
    }

    @EventHandler
    public void onLogout(PlayerQuitEvent event){
        unregisterPlayerToAllChannels(event.getPlayer());
    }


    @EventHandler
    public void onLogin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // If we have a remembered list for this player (unlikely if we cleared on quit),
        // restore them to those channels first.
        List<String> remembered = listening_channels.get(player.getUniqueId());
        if (remembered != null && !remembered.isEmpty()) {
            for (String ch : remembered) {
                getOrCreateChannelPlayerSet(ch, false).add(player);
            }
        }
    }

    /**
     * Establishes known/global default values.
     * If adding a new channel, specify it here.
     */
    private void setupDefaultChannels() {
        getOrCreateChannelPlayerSet("recipes", true);
        getOrCreateChannelPlayerSet("xp", true);
        getOrCreateChannelPlayerSet("damage", true);
        getOrCreateChannelPlayerSet("armor", true);
        getOrCreateChannelPlayerSet("weight", true);
        getOrCreateChannelPlayerSet("chat", true);
        getOrCreateChannelPlayerSet("levelup", true);
    }

    /**
     * returns if a player is listening to a specific debug channel
     * useful for quickly determining if a debug message should even be built
     * to prevent complex debug messages for being sent
     * see CustomPlayer.java for an example of why this is optimal
     */
    public static boolean isListeningToChannel(Player player, String debug_channel) {
        Debug debug = getInstance();
        if(debug.listening_channels.containsKey(player.getUniqueId())){
            if(debug.listening_channels.get(player.getUniqueId()).contains(debug_channel)){
                return true;
            }
            return false;
        }
        return false;
    }

    /**
     * Determines if anyone is listening to this channel before sending a message to it
     * Good to use if sending a detailed debug message
     */
    public static boolean isAnyoneListening(String debug_channel, boolean create_channel_regardless) {
        Debug debug = getInstance();
        if(!debug.debug_listening.containsKey(debug_channel)) {
            if(create_channel_regardless){
                debug.debug_listening.put(debug_channel, new HashSet<>());
            }
            return false;
        }
        return !debug.debug_listening.get(debug_channel).isEmpty();
    }

    static void resetAllValues(CommandSender commander) {
        Debug debug = getInstance();
        debug.debug_listening = new HashMap<String, Set<Player>>();
        debug.listening_channels = new HashMap<UUID, List<String>>();
        debug.debug_channels = new ArrayList<String>();
        debug.setupDefaultChannels();
        Specialization.getInstance().getLogger().info("Debug Cache Globally Reset by "+commander.getName());
    }

    /**
     * This registers a player to a debug channel to listen to it
     */
    public void registerPlayerChannel(Player player, String debug_channel){
        if (!player.hasPermission("specialization.debug")) {
            return;
        }
        Set<Player> player_set = getOrCreateChannelPlayerSet(debug_channel, false);
        player_set.add(player);
        listening_channels.computeIfAbsent(player.getUniqueId(), p -> new ArrayList<String>()).add(debug_channel);
    }

    /**
     * This unregisters a player fromm the channel they're listening to
     */
    public void unregisterPlayerChannel(Player player, String debug_channel){
        debug_channel = debug_channel.toLowerCase();
        Set<Player> player_set = getOrCreateChannelPlayerSet(debug_channel, false);
        player_set.remove(player);
        listening_channels.computeIfAbsent(player.getUniqueId(), p -> new ArrayList<String>()).remove(debug_channel);
    }

    public void registerPlayerToAllChannels(Player player) {
        for(String channel : debug_channels){
            registerPlayerChannel(player, channel);
        }
    }

    public void unregisterPlayerToAllChannels(Player player) {
        for(String channel : debug_channels){
            unregisterPlayerChannel(player, channel);
        }
    }

    private Set<Player> getOrCreateChannelPlayerSet(String debug_channel, boolean add_to_suggestions){
        debug_channel = debug_channel.toLowerCase();
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
        debug_channel = debug_channel.toLowerCase();
        for(Player player : getInstance().getOrCreateChannelPlayerSet(debug_channel, register_channel)){
            player.sendMessage(comp);
        }
    }

    public static void broadcast(String debug_channel, String msg){
        broadcast(debug_channel, msg, null);
    }


    public static void message(Player player, String debug_channel, String msg){
        message(player, getPrefix(debug_channel).toString() + debug_channel, msg, null);
    }

    /**
     * Sends a debug to a player who is listening to a debug channel
     */
    public static void message(Player player, String debug_channel, String msg, String hover_details){
        Debug debug = getInstance();
        Component comp = debug.formatDebugMessageDefault(debug_channel, msg, hover_details);
        if(debug.getOrCreateChannelPlayerSet(debug_channel, false).contains(player)){
            player.sendMessage(comp);
        }
    }

    /**
     * Sends a debug to a player who is listening to a debug channel
     */
    public static void message(Player player, String debug_channel, Component msg, Component hover) {
     Debug debug = getInstance();
        if(hover!=null){
            msg = msg.hoverEvent(HoverEvent.showText(hover));
        }
        if(debug.getOrCreateChannelPlayerSet(debug_channel, false).contains(player)){
            player.sendMessage(getPrefix(debug_channel).append(msg));
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
        return MiniMessage.miniMessage().deserialize("<dark_gray>[" + debug_channel.toLowerCase() + "]:</dark_gray> ");
    }

    /**
     * Used for Tab Completion with the command
     */
    public static List<String> getChannelList(){
        return getInstance().debug_channels;
    }

    public static List<String> getPlayerChannels(Player player){
        Debug debug = getInstance();
        if(debug.listening_channels.containsKey(player.getUniqueId())){
            return debug.listening_channels.get(player.getUniqueId());
        }
        return new ArrayList<String>();
    }

    public static Debug getInstance(){
        return Specialization.getInstance().getDebugUtils();
    }



    private static TextColor red = TextColor.color(1, 0.7f, 0.7f);
    private static TextColor green = TextColor.color(0.7f, 1, 0.7f);
    private static TextColor blue = TextColor.color(0.7f, 0.7f, 1);

    /**
     *
     */
    public static String formatLocation(Location location){
        return "<gray>"+location.getWorld().getName()+"</gray>,"
                +"<red>"+(int)location.getX()+"</red>, "
                +"<green>"+(int)location.getY()+"</green>, "
                +"<blue>"+(int)location.getZ()+"</blue>";
    }
    /**
     *
     */
    public static Component formatLocationColored(Location location){
        return MiniMessage.miniMessage().deserialize("<gray>"+location.getWorld().getName()+"[</gray>"
                +"<red>"+(int)location.getX()+"</red>, "
                +"<green>"+(int)location.getY()+"</green>, "
                +"<blue>"+(int)location.getZ()+"</blue><gray>]</gray>");
    }

    /**
     * Creates a clickable location
     */
    public static Component formatLocationClickable(Location location, boolean compact){
        Component c;
        if(compact) {
            c = MiniMessage.miniMessage().deserialize("<blue>[loc]</blue>");
            c = c.hoverEvent(formatLocationColored(location));
        }else{
            c = formatLocationColored(location);
        }
        return c.clickEvent(ClickEvent.suggestCommand("/tp "+location.getBlockX()+" "+location.getBlockY()+" "+location.getBlockZ()));
    }


    private static DecimalFormat decimal_format = new DecimalFormat("#.##");

    /**
     * Helper for formatting decimals
     */
    public static String formatDecimal(double d){
        return decimal_format.format(d);
    }


}