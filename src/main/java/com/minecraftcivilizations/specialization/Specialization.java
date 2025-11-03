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
import com.minecraftcivilizations.specialization.Listener.Blocks.AutoCrafterListener;
import com.minecraftcivilizations.specialization.Listener.BurnListener;
import com.minecraftcivilizations.specialization.Listener.Blocks.ReinforcementProtectionListener;
import com.minecraftcivilizations.specialization.Listener.Mobs.ExplodeListener;
import com.minecraftcivilizations.specialization.Listener.Mobs.MobKillListener;
import com.minecraftcivilizations.specialization.Listener.Mobs.MobListeners;
import com.minecraftcivilizations.specialization.Listener.Player.*;
import com.minecraftcivilizations.specialization.Listener.Player.Blocks.Mining.BreakBlockListener;
import com.minecraftcivilizations.specialization.Listener.Player.Blocks.Mining.PlayerMineListener;
import com.minecraftcivilizations.specialization.Listener.Player.Blocks.PlaceBlockListener;
import com.minecraftcivilizations.specialization.Listener.Player.Combat.ArmorDamageReductionListener;
import com.minecraftcivilizations.specialization.Listener.Player.Combat.Berserk;
import com.minecraftcivilizations.specialization.Listener.Player.Combat.CrossBowListener;
import com.minecraftcivilizations.specialization.Listener.Player.Combat.PatDown;
import com.minecraftcivilizations.specialization.Listener.Player.Interactions.FoodInteractionListener;
import com.minecraftcivilizations.specialization.Listener.Player.Interactions.PlayerInteractEntityListener;
import com.minecraftcivilizations.specialization.Listener.Player.Interactions.PlayerInteractListener;
import com.minecraftcivilizations.specialization.Listener.Player.Interactions.RightClickListener;
import com.minecraftcivilizations.specialization.Listener.Player.Interactions.*;
import com.minecraftcivilizations.specialization.Listener.Player.Inventories.CraftingListener;
import com.minecraftcivilizations.specialization.Listener.Player.Inventories.FurnaceListener;
import com.minecraftcivilizations.specialization.Listener.Player.Inventories.StonecutterListener;
import com.minecraftcivilizations.specialization.Listener.RepairingListener;
import com.minecraftcivilizations.specialization.Listener.XpTransferBookListener;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Player.LocalNameGenerator;
import com.minecraftcivilizations.specialization.Player.PreJoinEventListener;
//import com.minecraftcivilizations.specialization.Player.TeamManager;
import com.minecraftcivilizations.specialization.Recipe.Blueprints;
import com.minecraftcivilizations.specialization.Recipe.Recipes;
import com.minecraftcivilizations.specialization.Reinforcement.ReinforcementManager;
import com.minecraftcivilizations.specialization.Skill.Skill;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.SmartEntity.SmartEntityManager;
import com.minecraftcivilizations.specialization.StaffTools.DebugListenCommand;
import com.minecraftcivilizations.specialization.StaffTools.Debug;
import com.minecraftcivilizations.specialization.util.LocatorBarManager;
import com.mojang.authlib.GameProfile;
import minecraftcivilizations.com.minecraftCivilizationsCore.Component.ComponentUtils;
import minecraftcivilizations.com.minecraftCivilizationsCore.MinecraftCivilizationsCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class Specialization extends JavaPlugin {

    public static Logger logger;
    private LocalNameGenerator localNameGenerator;
    private Debug debug;

    SmartEntityManager smart_entity_manager;

    @Override
    public void onEnable() {
        logger = getLogger();

        debug = new Debug();
        saveResource("first_names.txt", false);
        saveResource("last_names.txt", false);
        SpecializationConfig.initialize();
        MongoConnection.startDBConnection();
        // TODO PDC-xp-hotfix
        //  Skill.InitializeSkillKeys(this);

        smart_entity_manager = new SmartEntityManager(this);

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
        getServer().getPluginManager().registerEvents(new FishingListener(), this);
        getServer().getPluginManager().registerEvents(new MobKillListener(), this);
        getServer().getPluginManager().registerEvents(new FoodInteractionListener(), this);
        getServer().getPluginManager().registerEvents(new HungerSystemListener(this), this);
        getServer().getPluginManager().registerEvents(new LeashListener(), this);
        getServer().getPluginManager().registerEvents(new BedListener(), this);
        getServer().getPluginManager().registerEvents(new LocatorBarManager(this), this);
        getServer().getPluginManager().registerEvents(new ReinforcementProtectionListener(), this);
        getServer().getPluginManager().registerEvents(new PreJoinEventListener(), this);
        getServer().getPluginManager().registerEvents(new StonecutterListener(this), this);
        getServer().getPluginManager().registerEvents(new CraftingListener(this), this);
        getServer().getPluginManager().registerEvents(new FurnaceListener(), this);
        getServer().getPluginManager().registerEvents(new AutoCrafterListener(), this);

        new TownManager();
        getServer().getPluginManager().registerEvents(new MoveListener(), this);
        getServer().getPluginManager().registerEvents(new CrossBowListener(), this);
        getServer().getPluginManager().registerEvents(new LocalChat(), this);
        getServer().getPluginManager().registerEvents(new MobListeners(), this);
        getServer().getPluginManager().registerEvents(new Berserk(), this);
        getServer().getPluginManager().registerEvents(new ArmorDamageReductionListener(), this);
        getServer().getPluginManager().registerEvents(new PatDown(), this);
        getServer().getPluginManager().registerEvents(new XpTransferBookListener(), this);
        getServer().getPluginManager().registerEvents(new RepairingListener(), this);
        getServer().getPluginManager().registerEvents(new RepairingListener(), this);
        getServer().getPluginManager().registerEvents(new PhantomRideListener(), this);

        //town data does not need to wait anymore
        TownManager.scanAllPlayersForTownsAsync();




        //overworld game rules
        World world = Bukkit.getWorlds().get(0);
        world.setGameRule(GameRule.SPAWN_RADIUS, 350);
        world.setGameRule(GameRule.REDUCED_DEBUG_INFO, true);
        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        world.setGameRule(GameRule.NATURAL_REGENERATION, false);
        world.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false);
        world.setGameRule(GameRule.LOCATOR_BAR, true);
        world.setGameRule(GameRule.WATER_SOURCE_CONVERSION, false);
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);

        //global game rules
        Bukkit.getWorlds().forEach(w -> w.setGameRule(GameRule.NATURAL_REGENERATION, false));
        Bukkit.getWorlds().forEach(w -> w.setGameRule(GameRule.DO_TRADER_SPAWNING, false));

        Recipes.init();
        Blueprints.init();
        XpGainMonitor.init();

        Bukkit.updateRecipes();


        MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().setCustomPlayerClass(CustomPlayer.class);

        MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().setOnPrePlayerJoin(playerJoinEvent -> {
            try {
                CustomPlayer load = (CustomPlayer) MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().load(playerJoinEvent.getUniqueId());
                Component localName;
                if (load != null) {
                    MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().addCustomPlayer(load);
                    localName = load.getName();
                } else {
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
                logger.severe("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
                e.printStackTrace();
            }
        });

        MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().setOnPlayerJoin(playerJoinEvent -> {
            CustomPlayer customPlayer = (CustomPlayer) MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().getCustomPlayer(playerJoinEvent.getUniqueId());
            applyCustomName(playerJoinEvent.getPlayer(), customPlayer.getName());
            customPlayer.applyEffects();

            // Migrate old bandages to new format
            Bukkit.getScheduler().runTaskLater(Specialization.getInstance(), () -> {
                migrateLegacyItems(playerJoinEvent.getPlayer());
            }, 10L);

            // TODO PDC-xp-hotfix for later if we need it
            //  customPlayer.reloadSkillsXp(playerJoinEvent.getPlayer());

            // Assign player to team based on their highest skill

//            TeamManager.setTeam(playerJoinEvent.getPlayer());

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
            CustomPlayer loadedPlayer = (CustomPlayer) MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().load(player.getUniqueId());
            if (loadedPlayer != null) {
                MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().addCustomPlayer(loadedPlayer);
            }
        }

        DataManager.startSaver(this);
        ReinforcementManager.startReinforcement();

        AnalyticsData.autoPoll();
    }


    @Override
    public void onDisable() {
        // Plugin shutdown logic
        smart_entity_manager.shutdown();
        DataManager.getScheduler().shutdown();
        MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().saveAll();
    }

    public static Specialization getInstance() {
        return getPlugin(Specialization.class);
    }

    PaperCommandManager commandManager;


    private void setupCommands() {

        try {
            localNameGenerator = new LocalNameGenerator(this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Cleans up optional names held in temp reserves
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            localNameGenerator.cleanupExpiredTemps();
        }, 0L, 10 * 60 * 20L); // every 10 minutes


        commandManager = new PaperCommandManager(this);


        // --- TAB COMPLETIONS ---
        commandManager.getCommandCompletions().registerCompletion("classes", c ->
                Arrays.stream(SkillType.values())
                        .map(Enum::name)
                        .collect(Collectors.toList())
        );
        commandManager.registerCommand(new ClassCommand());
        commandManager.registerCommand(new SetXpCommand());
        commandManager.registerCommand(new SetLoreCommand());
        commandManager.registerCommand(new TownsCommand());
        commandManager.registerCommand(new SuicideCommand());
        commandManager.registerCommand(new AnalyticsCommand());
        commandManager.registerCommand(new RestoreHealthCommand());
        commandManager.registerCommand(new NotifyRestartCommand());
        commandManager.registerCommand(new RecipesCommand());
        commandManager.registerCommand(new PurgeGoldenApplesCommand());
        commandManager.registerCommand(new RandomNameBulkTestCommand());
        commandManager.registerCommand(new RerollNameCommand(localNameGenerator));
        commandManager.registerCommand(new NameChoiceCommand(localNameGenerator));
        commandManager.registerCommand(new XPLeaderboardCommand());
        new DebugListenCommand(commandManager);


    }


    public void applyCustomName(Player player, Component name) {
        CustomPlayer customPlayer = (CustomPlayer) MinecraftCivilizationsCore
                .getInstance()
                .getCustomPlayerManager()
                .getCustomPlayer(player.getUniqueId());

        // Update the CustomPlayer's stored name
        customPlayer.setName(name);

        // Send the packet to all online players to update the display name
        PacketContainer packet = createChangeNamePacket(player.getUniqueId(), name);
        for (Player p : Bukkit.getOnlinePlayers()) {
            ProtocolLibrary.getProtocolManager().sendServerPacket(p, packet);
            // Also send each other player's custom name to the target player
            CustomPlayer otherPlayer = (CustomPlayer) MinecraftCivilizationsCore
                    .getInstance()
                    .getCustomPlayerManager()
                    .getCustomPlayer(p.getUniqueId());
            if (otherPlayer != null) {
                ProtocolLibrary.getProtocolManager().sendServerPacket(player,
                        createChangeNamePacket(p.getUniqueId(), otherPlayer.getName()));
            }
        }

        // Update the internal GameProfile to ensure name persists correctly
        try {
            ServerPlayer profile = ((CraftPlayer) player).getHandle();
            GameProfile gameProfile = profile.getGameProfile();
            Field nameField = gameProfile.getClass().getDeclaredField("name");
            nameField.setAccessible(true);
            nameField.set(gameProfile, ComponentUtils.serializeComponentAsString(name));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        // Force client to refresh player to avoid caching issues
        Bukkit.getScheduler().runTask(Specialization.getInstance(), () -> {
            for (Player all : Bukkit.getOnlinePlayers()) {
                all.hidePlayer(Specialization.getInstance(), player);
                all.showPlayer(Specialization.getInstance(), player);
            }
        });
    }


    private PacketContainer createChangeNamePacket(UUID uuid, Component name) {
        PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_INFO);

        // Update the display name only
        packet.getPlayerInfoActions().write(0,
                Collections.singleton(EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME));

        WrappedGameProfile profile = new WrappedGameProfile(uuid, ComponentUtils.serializeComponentAsString(name));
        WrappedChatComponent nameComponent = WrappedChatComponent.fromJson(
                JSONComponentSerializer.json().serialize(name)
        );

        List<PlayerInfoData> playerInfoData = List.of(
                new PlayerInfoData(profile, 0, EnumWrappers.NativeGameMode.SURVIVAL, nameComponent)
        );
        packet.getPlayerInfoDataLists().write(1, playerInfoData);

        return packet;
    }

    private void migrateLegacyItems(Player player) {
        int migratedCount = 0;
        
        for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.PAPER) {
                Component displayName = item.displayName();
                String plainName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(displayName);
                if (plainName.contains("Bandage")) {
                    NamespacedKey bandageKey = new NamespacedKey(Specialization.getInstance(), "bandage");
                    minecraftcivilizations.com.minecraftCivilizationsCore.Item.CustomItem newBandage = 
                        minecraftcivilizations.com.minecraftCivilizationsCore.Item.CustomItemRegistry.getItem(bandageKey);
                    
                    if (newBandage != null) {
                        int amount = item.getAmount();
                        org.bukkit.inventory.ItemStack newItem = newBandage.getItem();
                        newItem.setAmount(amount);
                        player.getInventory().remove(item);
                        player.getInventory().addItem(newItem);
                        migratedCount++;
                    }
                }
            }
        }
        
        if (migratedCount > 0) {
            player.sendMessage(Component.text("Migrated " + migratedCount + " old bandage(s) to new format").color(NamedTextColor.YELLOW));
            Bukkit.getLogger().info("[Migration] Migrated " + migratedCount + " bandages for player " + player.getName());
        }
    }

    public Debug getDebugUtils() {
        return debug;
    }
}
