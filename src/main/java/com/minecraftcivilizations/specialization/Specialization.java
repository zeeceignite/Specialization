package com.minecraftcivilizations.specialization;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.google.gson.Gson;
import com.minecraftcivilizations.specialization.Command.ClassCommandExecutor;
import com.minecraftcivilizations.specialization.Command.ReloadPluginCommandExecutor;
import com.minecraftcivilizations.specialization.Command.SetLoreCommandExecutor;
import com.minecraftcivilizations.specialization.Command.SetXpCommandExecutor;
import com.minecraftcivilizations.specialization.Config.Config;
import com.minecraftcivilizations.specialization.Data.DataManager;
import com.minecraftcivilizations.specialization.Listener.BreakBlockListener;
import com.minecraftcivilizations.specialization.Listener.FurnaceListener;
import com.minecraftcivilizations.specialization.Listener.PlayerMineListener;
import com.minecraftcivilizations.specialization.Listener.StonecutterListener;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Player.PreJoinEventListener;
import com.mojang.authlib.GameProfile;
import lombok.Getter;
import minecraftcivilizations.com.minecraftCivilizationsCore.Component.ComponentUtils;
import minecraftcivilizations.com.minecraftCivilizationsCore.MinecraftCivilizationsCore;
import minecraftcivilizations.com.minecraftCivilizationsCore.ProtocolLib.PacketManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.data.type.Grindstone;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.inventory.CampfireRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public final class Specialization extends JavaPlugin {
    public static Logger logger;
    @Override
    public void onEnable() {
        logger = getLogger();

        getServer().getPluginCommand("class").setExecutor(new ClassCommandExecutor());
        getServer().getPluginCommand("setxp").setExecutor(new SetXpCommandExecutor());
        getServer().getPluginCommand("reloadconfigs").setExecutor(new ReloadPluginCommandExecutor());
        getServer().getPluginCommand("setlore").setExecutor(new SetLoreCommandExecutor());

        getServer().getPluginManager().registerEvents(new PlayerMineListener(), this);
        getServer().getPluginManager().registerEvents(new BreakBlockListener(), this);

        getServer().getPluginManager().registerEvents(new StonecutterListener(), this);
        getServer().getPluginManager().registerEvents(new FurnaceListener(), this);
        getServer().getPluginManager().registerEvents(new PreJoinEventListener(), this);


        ItemStack result = new ItemStack(Material.DIAMOND_SWORD);

        // Unique recipe key for registry
        NamespacedKey key = new NamespacedKey(this, "diamond_sword_plus");

        ShapedRecipe recipe = new ShapedRecipe(key, result);

        // Recipe shape
        recipe.shape(" D ", " D ", " S ");

        // Ingredients
        recipe.setIngredient('D', Material.DIAMOND_BLOCK);
        recipe.setIngredient('S', Material.STICK);

        // Register the recipe
        Bukkit.addRecipe(recipe);


        MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().setOnPlayerJoin(playerJoinEvent -> {
            MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().addCustomPlayer(new CustomPlayer(playerJoinEvent.getUniqueId()));

            Specialization.logger.info("Custom player joined!");

            CustomPlayer customPlayer = (CustomPlayer) MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().getCustomPlayer(playerJoinEvent.getUniqueId());

            Specialization.logger.info("Custom player joined!");

            applyCustomName(playerJoinEvent.getPlayer(), Component.text("DUMBASS").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));


        });

        MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().setOnPlayerQuit(playerQuitEvent -> {
            MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().removeCustomPlayer(playerQuitEvent.getPlayer().getUniqueId());
        });

        for (Player player : Bukkit.getOnlinePlayers()) {
            MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().addCustomPlayer(new CustomPlayer(player.getUniqueId()));
        }

        DataManager.startSaver();
        Config.initialize();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }


    public static Specialization getInstance() {
        return getPlugin(Specialization.class);
    }








    public void applyCustomName(Player player, Component name){
        PacketContainer packet = createChangeNamePacket(player.getUniqueId(), name);
        CustomPlayer customPlayer = (CustomPlayer) MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().getCustomPlayer(player.getUniqueId());
        for (Player p : Bukkit.getServer().getOnlinePlayers()) {
            ProtocolLibrary.getProtocolManager().sendServerPacket(p, packet); // show everyone your name
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, createChangeNamePacket(p.getUniqueId(), Component.text("DUMBASS"))); // everyone tells you their name
        }
        try {
            ServerPlayer profile = ((CraftPlayer) player).getHandle();
            GameProfile gameProfile = profile.getGameProfile();
            Field ff = gameProfile.getClass().getDeclaredField("name");
            ff.setAccessible(true);
            ff.set(gameProfile, "DUMBASS");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        Bukkit.getScheduler().runTask(Specialization.getInstance(), ()-> {
            for (Player all : Bukkit.getOnlinePlayers()) {
                all.hidePlayer(Specialization.getInstance(), player);
                all.showPlayer(Specialization.getInstance(), player);
            }
        });
    }

    private PacketContainer createChangeNamePacket(UUID uuid, Component name) {
        PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_INFO);
        packet.getPlayerInfoActions().write(0, Collections.singleton(EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME));
        List<PlayerInfoData> pd = new ArrayList<>();
        WrappedGameProfile profile = new WrappedGameProfile(uuid, ComponentUtils.serializeComponentAsString(name));
        WrappedChatComponent nameComponent = WrappedChatComponent.fromJson(JSONComponentSerializer.json().serialize(Component.text("DUMBASS")));
        pd.add(new PlayerInfoData(profile, 0, EnumWrappers.NativeGameMode.SURVIVAL, nameComponent));
        packet.getPlayerInfoDataLists().write(1, pd);
        return packet;
    }
}
