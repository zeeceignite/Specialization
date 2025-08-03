package com.minecraftcivilizations.specialization.util;

import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import minecraftcivilizations.com.minecraftCivilizationsCore.MinecraftCivilizationsCore;
import org.bukkit.entity.HumanEntity;

import java.util.UUID;

public class CoreUtil {
    public static CustomPlayer getPlayer(UUID uuid){
        return (CustomPlayer) MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().getCustomPlayer(uuid);
    }
    public static CustomPlayer getPlayer(HumanEntity player){
        return getPlayer(player.getUniqueId());
    }
}
