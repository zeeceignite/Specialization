package com.minecraftcivilizations.specialization.Combat;

import com.minecraftcivilizations.specialization.GUI.PatDownGUI;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class PatDown implements Listener {
    
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player inspector = event.getPlayer();
        

        CustomPlayer customInspector = CoreUtil.getPlayer(inspector);
        if (customInspector.getSkillLevel(SkillType.GUARDSMAN) < 1) {
            return;
        }
        

        if (!inspector.isSneaking()) {
            return;
        }

        if (!(event.getRightClicked() instanceof Player target)) {
            return;
        }

        if (inspector.equals(target)) {
            return;
        }
        
        event.setCancelled(true);

        new PatDownGUI(inspector, target).open(inspector);
    }
}
