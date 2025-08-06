package com.minecraftcivilizations.specialization.Listener;

import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillLevel;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.Specialization;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class PlayerInteractEntityListener implements Listener {
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
        Player p = e.getPlayer();
        if (e.getRightClicked() instanceof Player clickedPlayer) {
            CustomPlayer customPlayer = CoreUtil.getPlayer(p.getUniqueId());
            if (customPlayer == null) return;
            if (customPlayer.getSkillLevel(SkillType.HEALER) > SkillLevel.JOURNEYMAN.getLevel()) {
                CustomPlayer downedPlayer = CoreUtil.getPlayer(clickedPlayer.getUniqueId());
                if (downedPlayer != null && downedPlayer.isDowned()) {
                    PlayerDeathListener.removeDownedArmorStand(clickedPlayer);
                    p.addPassenger(clickedPlayer);
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (p.getPassengers().isEmpty()) {
                                p.removePotionEffect(PotionEffectType.SLOWNESS);
                                return;
                            }
                            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 2));
                        }
                    }.runTaskTimer(Specialization.getInstance(), 0, 1);
                }
            }
        }
    }
}