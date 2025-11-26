package com.minecraftcivilizations.specialization.Combat;

import com.minecraftcivilizations.specialization.GUI.PatDownGUI;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.Specialization;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class PatDown implements Listener {
    
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player inspector = event.getPlayer();
        if (!(event.getRightClicked() instanceof Player target)) {return;}
        CustomPlayer customInspector = CoreUtil.getPlayer(inspector);
        // Check target PDC for downed
        // Check target PDC for downed (BYTE)
        NamespacedKey downedKey = new NamespacedKey(Specialization.getInstance(), "is_downed");
        PersistentDataContainer pdc = target.getPersistentDataContainer();
        boolean isDowned = pdc.has(downedKey, PersistentDataType.BYTE) && pdc.get(downedKey, PersistentDataType.BYTE) == 1;

        // Return only if inspector is NOT a Guardsman AND target is NOT downed
        if (customInspector.getSkillLevel(SkillType.GUARDSMAN) < 1 && !isDowned) return;
        if (!inspector.isSneaking()) {return;}
        if (inspector.equals(target)) {return;}
        event.setCancelled(true);

        new PatDownGUI(inspector, target).open(inspector);
    }
}
