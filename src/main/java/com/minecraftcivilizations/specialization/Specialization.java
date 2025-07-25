package com.minecraftcivilizations.specialization;

import com.minecraftcivilizations.specialization.Command.ClassCommandExecutor;
import com.minecraftcivilizations.specialization.Command.ReloadPluginCommandExecutor;
import com.minecraftcivilizations.specialization.Command.SetLoreCommandExecutor;
import com.minecraftcivilizations.specialization.Command.SetXpCommandExecutor;
import com.minecraftcivilizations.specialization.Config.Config;
import com.minecraftcivilizations.specialization.Data.DataManager;
import com.minecraftcivilizations.specialization.Listener.BreakBlockListener;
import com.minecraftcivilizations.specialization.Listener.PlayerMineListener;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import lombok.Getter;
import minecraftcivilizations.com.minecraftCivilizationsCore.MinecraftCivilizationsCore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.data.type.Grindstone;
import org.bukkit.entity.Player;
import org.bukkit.inventory.CampfireRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
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
            MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().addCustomPlayer(new CustomPlayer(playerJoinEvent.getPlayer().getUniqueId()));
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
}
