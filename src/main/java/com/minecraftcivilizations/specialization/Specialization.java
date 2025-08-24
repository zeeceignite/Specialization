package com.minecraftcivilizations.specialization;

import co.aikar.commands.PaperCommandManager;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.destroystokyo.paper.profile.CraftPlayerProfile;
import com.minecraftcivilizations.specialization.Analytics.AnalyticsData;
import com.minecraftcivilizations.specialization.Command.*;
import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Data.DataManager;
import com.minecraftcivilizations.specialization.Data.MongoConnection;
import com.minecraftcivilizations.specialization.Distance.TownManager;
import com.minecraftcivilizations.specialization.Listener.BurnListener;
import com.minecraftcivilizations.specialization.Listener.Player.BedListener;
import com.minecraftcivilizations.specialization.Listener.Mobs.ExplodeListener;
import com.minecraftcivilizations.specialization.Listener.Mobs.MobKillListener;
import com.minecraftcivilizations.specialization.Listener.Mobs.MobListeners;
import com.minecraftcivilizations.specialization.Listener.Player.Blocks.Mining.BreakBlockListener;
import com.minecraftcivilizations.specialization.Listener.Player.Blocks.Mining.PlayerMineListener;
import com.minecraftcivilizations.specialization.Listener.Player.Blocks.PlaceBlockListener;
import com.minecraftcivilizations.specialization.Listener.Player.Combat.ArmorDamageReductionListener;
import com.minecraftcivilizations.specialization.Listener.Player.Combat.Berserk;
import com.minecraftcivilizations.specialization.Listener.Player.Combat.CrossBowListener;
import com.minecraftcivilizations.specialization.Listener.Player.*;
import com.minecraftcivilizations.specialization.Listener.Player.Interactions.FoodInteractionListener;
import com.minecraftcivilizations.specialization.Listener.Player.Interactions.PlayerInteractEntityListener;
import com.minecraftcivilizations.specialization.Listener.Player.Interactions.PlayerInteractListener;
import com.minecraftcivilizations.specialization.Listener.Player.Interactions.RightClickListener;
import com.minecraftcivilizations.specialization.Listener.Player.Inventories.CraftingListener;
import com.minecraftcivilizations.specialization.Listener.Player.Inventories.FurnaceListener;
import com.minecraftcivilizations.specialization.Listener.Player.Inventories.StonecutterListener;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Player.LocalNameGenerator;
import com.minecraftcivilizations.specialization.Player.PreJoinEventListener;
import com.minecraftcivilizations.specialization.Player.TeamManager;
import com.minecraftcivilizations.specialization.Recipe.Blueprints;
import com.minecraftcivilizations.specialization.Recipe.Recipes;
import com.minecraftcivilizations.specialization.Reinforcement.ReinforcementManager;
import com.minecraftcivilizations.specialization.Skill.Skill;
import com.mojang.authlib.GameProfile;
import minecraftcivilizations.com.minecraftCivilizationsCore.Component.ComponentUtils;
import minecraftcivilizations.com.minecraftCivilizationsCore.MinecraftCivilizationsCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public final class Specialization extends JavaPlugin {

    public static Logger logger;
    private static LocalNameGenerator localNameGenerator;

    @Override
    public void onEnable() {

        logger = getLogger();

        saveResource("first_names.txt", false);
        saveResource("last_names.txt", false);
        SpecializationConfig.initialize();

        MongoConnection.startDBConnection();

        TeamManager.initializeTeams();

        setupCommands();

        getServer().getPluginManager().registerEvents(new PlayerMineListener(), this);
        getServer().getPluginManager().registerEvents(new BreakBlockListener(), this);
        getServer().getPluginManager().registerEvents(new PlaceBlockListener(), this);
        getServer().getPluginManager().registerEvents(new RightClickListener(), this);
        getServer().getPluginManager().registerEvents(new BurnListener(), this);
        getServer().getPluginManager().registerEvents(new ExplodeListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractEntityListener(), this);
        getServer().getPluginManager().registerEvents(new MobKillListener(), this);
        getServer().getPluginManager().registerEvents(new FoodInteractionListener(), this);
        getServer().getPluginManager().registerEvents(new HungerSystemListener(this), this);
        getServer().getPluginManager().registerEvents(new LeashListener(), this);
        getServer().getPluginManager().registerEvents(new BedListener(), this);

        getServer().getPluginManager().registerEvents(new StonecutterListener(), this);
        getServer().getPluginManager().registerEvents(new CraftingListener(this), this);
        getServer().getPluginManager().registerEvents(new FurnaceListener(), this);
        getServer().getPluginManager().registerEvents(new PreJoinEventListener(), this);
        getServer().getPluginManager().registerEvents(new TownManager(), this);
        getServer().getPluginManager().registerEvents(new MoveListener(), this);
        getServer().getPluginManager().registerEvents(new CrossBowListener(), this);
        getServer().getPluginManager().registerEvents(new LocalChat(), this);
        getServer().getPluginManager().registerEvents(new MobListeners(), this);
        getServer().getPluginManager().registerEvents(new Berserk(), this);
        getServer().getPluginManager().registerEvents(new ArmorDamageReductionListener(), this);


        new BukkitRunnable() {
            @Override
            public void run() {
                TownManager.scanAllPlayersForTowns();
            }
        }.runTaskAsynchronously(this);


        Recipes.init();
        Blueprints.init();

        Bukkit.updateRecipes();

        try {
            localNameGenerator = new LocalNameGenerator();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().setCustomPlayerClass(CustomPlayer.class);

        MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().setOnPrePlayerJoin(playerJoinEvent -> {
            try {
                CustomPlayer load = (CustomPlayer) MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().load(playerJoinEvent.getUniqueId());
                Component localName;
                if (load != null) {
                    Specialization.logger.info("Already joined before!");
                    localName = load.getName();
                } else {
                    Specialization.logger.info("Custom player not found!");
                    MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().addCustomPlayer(new CustomPlayer(playerJoinEvent.getUniqueId()));
                    CustomPlayer customPlayer = (CustomPlayer) MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().getCustomPlayer(playerJoinEvent.getUniqueId());
                    customPlayer.setName(Component.text(localNameGenerator.nextName()).color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
                    double height = Skill.mapValue(Math.random(), 0.0, 1.0, .85, 1.0);
                    customPlayer.setHeight(height);
                    localName = customPlayer.getName();
                }


                CraftPlayerProfile profile = (CraftPlayerProfile) playerJoinEvent.getPlayerProfile();
                GameProfile gameProfile = profile.getGameProfile();
                Field ff = gameProfile.getClass().getDeclaredField("name");
                ff.setAccessible(true);
                ff.set(gameProfile, ComponentUtils.serializeComponentAsString(localName));

            } catch (NoSuchFieldException | IllegalAccessException e) {
                Bukkit.getLogger().severe("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
                e.printStackTrace();
            }
        });

        MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().setOnPlayerJoin(playerJoinEvent -> {
            CustomPlayer customPlayer = (CustomPlayer) MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().getCustomPlayer(playerJoinEvent.getUniqueId());
            applyCustomName(playerJoinEvent.getPlayer(), customPlayer.getName());
            customPlayer.applyEffects();
            
            // Assign player to team based on their highest skill
            TeamManager.setTeam(playerJoinEvent.getPlayer());
            
            // Restore downed state if they were downed when they logged out
            if (customPlayer.isWasDownedOnLogout()) {
                // Use Bukkit.getScheduler() to delay this until after the player has fully joined
                Bukkit.getScheduler().runTaskLater(Specialization.getInstance(), () -> {
                    Player player = playerJoinEvent.getPlayer();
                    if (player != null && player.isOnline()) {
                        // Restore downed state without starting the death timer
                        PlayerDeathListener.restoreDownedState(player, customPlayer);
                        // Clear the flag since we've restored the state
                        customPlayer.setWasDownedOnLogout(false);
                    }
                }, 5L); // 5 ticks delay to ensure player is fully loaded
            }
        });

        MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().setOnPlayerQuit(playerQuitEvent -> {
            // Save downed state to restore on rejoin, then clean up current session state
            CustomPlayer customPlayer = (CustomPlayer) MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().getCustomPlayer(playerQuitEvent.getPlayer().getUniqueId());
            if (customPlayer != null) {
                if (customPlayer.isDowned()) {
                    // Save that they were downed when they logged out
                    customPlayer.setWasDownedOnLogout(true);
                    // Clean up current session state to prevent infinite death loop
                    customPlayer.setDowned(false);
                    PlayerDeathListener.removeDownedArmorStand(playerQuitEvent.getPlayer());
                } else {
                    // They weren't downed, so clear the flag
                    customPlayer.setWasDownedOnLogout(false);
                }
            }
            MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().removeCustomPlayer(playerQuitEvent.getPlayer().getUniqueId());
        });

        for (Player player : Bukkit.getOnlinePlayers()) {
            Specialization.logger.info("Loaded player: " + player.getName());
            MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().load(player.getUniqueId());
        }

        DataManager.startSaver();
        ReinforcementManager.startReinforcement();

        AnalyticsData.autoPoll();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        DataManager.getScheduler().shutdown();
        MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().saveAll();
    }

    public static Specialization getInstance() {
        return getPlugin(Specialization.class);
    }

    private void setupCommands(){
        PaperCommandManager commandManager = new PaperCommandManager(this);
        commandManager.registerCommand(new ClassCommand());
        commandManager.registerCommand(new SetXpCommand());
        commandManager.registerCommand(new SetLoreCommand());
        commandManager.registerCommand(new TownsCommand());
        commandManager.registerCommand(new SuicideCommand());
        commandManager.registerCommand(new AnalyticsCommand());
    }

    public void applyCustomName(Player player, Component name){
        PacketContainer packet = createChangeNamePacket(player.getUniqueId(), name);
        CustomPlayer customPlayer = (CustomPlayer) MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().getCustomPlayer(player.getUniqueId());
        for (Player p : Bukkit.getServer().getOnlinePlayers()) {
            ProtocolLibrary.getProtocolManager().sendServerPacket(p, packet); // show everyone your name
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, createChangeNamePacket(p.getUniqueId(), customPlayer.getName())); // everyone tells you their name
        }
        try {
            ServerPlayer profile = ((CraftPlayer) player).getHandle();
            GameProfile gameProfile = profile.getGameProfile();
            Field ff = gameProfile.getClass().getDeclaredField("name");
            ff.setAccessible(true);
            ff.set(gameProfile, ComponentUtils.serializeComponentAsString(customPlayer.getName()));
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

        WrappedGameProfile profile = new WrappedGameProfile(uuid, ComponentUtils.serializeComponentAsString(name));
        WrappedChatComponent nameComponent = WrappedChatComponent.fromJson(JSONComponentSerializer.json().serialize(Component.text("DUMBASS")));
        List<PlayerInfoData> playerInfoData = List.of(new PlayerInfoData(profile, 0, EnumWrappers.NativeGameMode.SURVIVAL, nameComponent));
        packet.getPlayerInfoDataLists().write(1, playerInfoData);

        return packet;
    }
}
