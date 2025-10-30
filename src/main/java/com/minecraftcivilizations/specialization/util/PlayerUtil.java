package com.minecraftcivilizations.specialization.util;

import org.bukkit.entity.Player;

/**
 * Utilities related to player management
 */
public class PlayerUtil {


    /**
     * @return true if the player has the requested xp and if it was consumed
     */
    public static boolean TryConsumeXp(Player player, int xp_amount){
        int current_xp = player.getTotalExperience();
        if(current_xp >= xp_amount){
            player.setTotalExperience(current_xp - xp_amount);
            return true;
        }
        return false;
    }

}
