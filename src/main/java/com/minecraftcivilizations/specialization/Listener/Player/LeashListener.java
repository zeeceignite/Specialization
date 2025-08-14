package com.minecraftcivilizations.specialization.Listener.Player;

import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LeashHitch;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.UUID;

public class LeashListener implements Listener {

    public static HashMap<Player, Sheep> activeLeashedMobs = new HashMap<>();

    @EventHandler
    public void onPlayerMoveEvent(PlayerMoveEvent e) {
        if(activeLeashedMobs.containsKey(e.getPlayer())){
            Location destination = activeLeashedMobs.get(e.getPlayer()).getLocation();
            destination.setYaw(e.getTo().getYaw());
            destination.setPitch(e.getTo().getPitch());
            e.getPlayer().teleport(destination);
        }
    }

    @EventHandler
    public void onTryToLeashPlayer(PlayerInteractAtEntityEvent e) {
        if(e.getRightClicked() instanceof Player target){
            if(activeLeashedMobs.containsKey(target)){
                Bukkit.getLogger().info("REMOVING: " + activeLeashedMobs.get(target).getName());
                activeLeashedMobs.remove(e.getPlayer()).remove();
                CustomPlayer pTarget = CoreUtil.getPlayer(target);
                pTarget.setLeashedTo(null);
                CustomPlayer cPlayer = CoreUtil.getPlayer(e.getPlayer());
                cPlayer.getLeashedOtherPlayers().remove(target.getUniqueId());
            }else{
                addTracker(e.getPlayer(), target);
            }
        }
    }


    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent e) {
        CustomPlayer player = CoreUtil.getPlayer(e);
        if(player.getLeashedTo() != null){
            if(Bukkit.getPlayer(player.getLeashedTo()) != null){
                addTracker(Bukkit.getPlayer(player.getLeashedTo()), e.getPlayer());
            }else{
                player.setLeashedTo(null);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e){
        if(activeLeashedMobs.containsKey(e.getPlayer())){
            activeLeashedMobs.remove(e.getPlayer()).remove();
        }
        CustomPlayer player = CoreUtil.getPlayer(e);
        if(player.getLeashedOtherPlayers() != null){
            for(UUID uuid : player.getLeashedOtherPlayers()){
                if(Bukkit.getPlayer(uuid) != null){
                    CustomPlayer customPlayer = CoreUtil.getPlayer(uuid);
                    customPlayer.setLeashedTo(null);
                    activeLeashedMobs.remove(Bukkit.getPlayer(uuid));
                }
            }
        }
    }

    @EventHandler
    public void onEntityDie(PlayerDeathEvent e) {
        CustomPlayer player = CoreUtil.getPlayer(e.getPlayer());
        if(player.getLeashedTo() != null){
            activeLeashedMobs.remove(e.getPlayer());
            if(Bukkit.getPlayer(player.getLeashedTo()) != null){
                CustomPlayer otherPlayer = CoreUtil.getPlayer(player.getLeashedTo());
                otherPlayer.getLeashedOtherPlayers().remove(e.getPlayer().getUniqueId());
            }
        }
        if(player.getLeashedOtherPlayers() != null){
            for(UUID uuid : player.getLeashedOtherPlayers()){
                if(Bukkit.getPlayer(uuid) != null){
                    activeLeashedMobs.remove(Bukkit.getPlayer(uuid));
                    CoreUtil.getPlayer(uuid).setLeashedTo(null);
                }
            }
        }
    }

    @EventHandler
    public void onTryLeashToBlock(PlayerLeashEntityEvent e){
        if(e.getLeashHolder() instanceof LeashHitch && CoreUtil.getPlayer(e.getEntity().getUniqueId()) != null && CoreUtil.getPlayer(e.getEntity().getUniqueId()).getLeashedTo() != null){
            e.setCancelled(true);
        }
    }

    private void addTracker(Player player, Player target){
        player.getInventory().getItemInMainHand().setAmount(player.getInventory().getItemInMainHand().getAmount() - 1);
        Sheep sheep = target.getWorld().spawn(player.getLocation(), Sheep.class, zombo -> {
            zombo.setInvisible(true);
            zombo.setInvulnerable(true);
            zombo.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 255, false, false));
            zombo.setLeashHolder(player);
        });
        Bukkit.getMobGoals().removeAllGoals(sheep);
        activeLeashedMobs.put(target, sheep);
        CoreUtil.getPlayer(target).setLeashedTo(player.getUniqueId());
        CoreUtil.getPlayer(player).getLeashedOtherPlayers().add(target.getUniqueId());
    }
}