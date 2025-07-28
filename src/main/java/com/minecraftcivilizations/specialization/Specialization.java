package com.minecraftcivilizations.specialization;

import co.aikar.commands.PaperCommandManager;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.minecraftcivilizations.specialization.Command.*;
import com.minecraftcivilizations.specialization.Config.Config;
import com.minecraftcivilizations.specialization.Data.DataManager;
import com.minecraftcivilizations.specialization.Distance.TownManager;
import com.minecraftcivilizations.specialization.Listener.*;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Player.PreJoinEventListener;
import com.minecraftcivilizations.specialization.Recipe.Recipes;
import com.mojang.authlib.GameProfile;
import minecraftcivilizations.com.minecraftCivilizationsCore.Component.ComponentUtils;
import minecraftcivilizations.com.minecraftCivilizationsCore.MinecraftCivilizationsCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

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

        setupCommands();

        getServer().getPluginManager().registerEvents(new PlayerMineListener(), this);
        getServer().getPluginManager().registerEvents(new BreakBlockListener(), this);

        getServer().getPluginManager().registerEvents(new StonecutterListener(), this);
        getServer().getPluginManager().registerEvents(new FurnaceListener(), this);
        getServer().getPluginManager().registerEvents(new PreJoinEventListener(), this);
        getServer().getPluginManager().registerEvents(new TownManager(), this);
        getServer().getPluginManager().registerEvents(new MoveListener(), this);

        new BukkitRunnable() {
            @Override
            public void run() {
                TownManager.scanAllPlayersForTowns();
            }
        }.runTaskAsynchronously(this);


        Recipes.init();

        Bukkit.updateRecipes();

        MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().setCustomPlayerClass(CustomPlayer.class);

        MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().setOnPlayerJoin(playerJoinEvent -> {
            minecraftcivilizations.com.minecraftCivilizationsCore.Player.CustomPlayer load = MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().load(playerJoinEvent.getUniqueId());

            if (load != null) {
                Specialization.logger.info("Custom player joined!");
                return;
            }

            
            MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().addCustomPlayer(new CustomPlayer(playerJoinEvent.getUniqueId()));
            CustomPlayer customPlayer = (CustomPlayer) MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().getCustomPlayer(playerJoinEvent.getUniqueId());

            Specialization.logger.info("Custom player joined!!");

//            applyCustomName(playerJoinEvent.getPlayer(), Component.text("DUMBASS").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));


        });

        MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().setOnPlayerQuit(playerQuitEvent -> MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().removeCustomPlayer(playerQuitEvent.getPlayer().getUniqueId()));

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

    private void setupCommands(){
        PaperCommandManager commandManager = new PaperCommandManager(this);
        commandManager.registerCommand(new ClassCommand());
        commandManager.registerCommand(new SetXpCommand());
        commandManager.registerCommand(new ReloadConfigCommand());
        commandManager.registerCommand(new SetLoreCommand());
        commandManager.registerCommand(new TownsCommand());
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
