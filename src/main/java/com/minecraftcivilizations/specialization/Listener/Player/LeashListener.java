package com.minecraftcivilizations.specialization.Listener.Player;

import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillLevel;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.Specialization;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityUnleashEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class LeashListener implements Listener {

    private final Map<Player, Snowman> proxies = new HashMap<>();
    private final PlayerDownedListener downedListener;
    NamespacedKey leashKey = new NamespacedKey(Specialization.getInstance(), "leash_proxy");

    public LeashListener() {
        this.downedListener = new PlayerDownedListener(Specialization.getInstance());
    }

    // -------------------------
    // Core leash logic
    // -------------------------
    @EventHandler
    public void onLeash(PlayerInteractAtEntityEvent e) {
        // Only handle main hand or off hand
        if (e.getHand() != EquipmentSlot.HAND) return;


        Entity target = e.getRightClicked();
        Player leasher = e.getPlayer();


        boolean isPlayerTarget = target instanceof Player;
        boolean isPillagerTarget = target instanceof Pillager && leasher.isOp(); //for testing



        // Shift-right-click: dismount/remove proxy
        if (leasher.isSneaking() && target.isInsideVehicle()) {
            if (isPlayerTarget || isPillagerTarget) {
                    target.getVehicle().remove();
                    target.leaveVehicle();

            }
            return; // done for shift-right-click
        }

        // Must hold lead in either hand
        if (leasher.getInventory().getItemInMainHand().getType() != Material.LEAD &&
                leasher.getInventory().getItemInOffHand().getType() != Material.LEAD) return;

        // Normal right-click: only leash
        if (target.isInsideVehicle()) return; //must not already be leashed or carried

        if (isPlayerTarget) {
            Player targetPlayer = (Player) target;
            CustomPlayer h = CoreUtil.getPlayer(leasher);

            if (h.getSkillLevel(SkillType.GUARDSMAN) < SkillLevel.GRANDMASTER.getLevel()) {
                leasher.sendMessage("Only grandmaster guardsmen can leash.");
                return;
            }

            if (!downedListener.isDowned(targetPlayer)) {
                leasher.sendMessage("Target must be downed.");
                return;
            }


            downedListener.clearMount(targetPlayer);
            Snowman proxy = spawnProxy(targetPlayer.getLocation(), leasher);
            proxy.addPassenger(targetPlayer);
            proxy.setLeashHolder(leasher);
            proxy.getPersistentDataContainer().set(leashKey, PersistentDataType.STRING, "true");
            proxies.put(targetPlayer, proxy);
            leasher.getInventory().getItemInMainHand().subtract(1);
            e.setCancelled(true);
        } else if (isPillagerTarget) {
            Snowman proxy = spawnProxy(target.getLocation(), leasher);
            proxy.addPassenger(target);
            proxy.setLeashHolder(leasher);
            proxy.getPersistentDataContainer().set(leashKey, PersistentDataType.STRING, "true");
            leasher.getInventory().getItemInMainHand().subtract(1);
            e.setCancelled(true);
        }
    }



    // -------------------------
    // Snowman despawn / unleash
    // -------------------------
    @EventHandler
    public void onUnleash(EntityUnleashEvent e) {
        if (!(e.getEntity() instanceof Snowman proxy)) return;
        if (!proxy.getPersistentDataContainer().has(leashKey, PersistentDataType.STRING)) return;

        // Dismount all passengers safely
        for (Entity passenger : proxy.getPassengers()) {
            passenger.leaveVehicle();
            if (passenger instanceof Player p && downedListener.isDowned(p)) {
                downedListener.setSit(p);
                proxies.remove(p); // remove from tracked map
            }
        }

        proxy.remove();
    }

    // -------------------------
    // Relog handling
    // -------------------------
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        Snowman proxy = proxies.get(p);
        if (proxy != null && proxy.isValid()) {
            p.addPassenger(proxy);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        Snowman proxy = proxies.get(p);
        if (proxy != null) {
            if (downedListener.isDowned(p)) {
                downedListener.setSit(p);
            }
            proxy.remove();
            proxies.remove(p);
        }
    }

    // -------------------------
    // Prevent snow layering
    // -------------------------
    @EventHandler
    public void onEntityBlockForm(EntityBlockFormEvent e) {
        if (!(e.getEntity() instanceof Snowman snowman)) return;
        if (!snowman.getPersistentDataContainer().has(leashKey, PersistentDataType.STRING)) return;

        if (e.getNewState().getType() == Material.SNOW) e.setCancelled(true);
    }

    // -------------------------
    // Proxy spawn utility
    // -------------------------
    private Snowman spawnProxy(Location loc, Player leasher) {
        Snowman s = loc.getWorld().spawn(loc, Snowman.class, sm -> {
            sm.setSilent(true);
            sm.setInvisible(true);
            sm.setAware(false);
            sm.setAI(true);
            sm.setGravity(true);
            sm.setInvulnerable(true);
            sm.setCanPickupItems(false);
            sm.setCollidable(false);
            sm.setGlowing(false);
            sm.getAttribute(Attribute.STEP_HEIGHT).setBaseValue(1.0);
            sm.getAttribute(Attribute.SCALE).setBaseValue(0.1);
            sm.getEquipment().clear();
//            sm.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, Integer.MAX_VALUE, 255, false, false));
//            sm.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 255, false, false));
        });
        return s;
    }


    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        Entity dead = e.getEntity();

        // If a player dies and has a tracked snowman
        if (dead instanceof Player p) {
            Snowman proxy = proxies.get(p);
            if (proxy != null && proxy.isValid()) {
                for (Entity passenger : proxy.getPassengers()) passenger.leaveVehicle();
                proxy.remove();
                proxies.remove(p);
            }
        }

        // If a pillager dies while riding a snowman
        if (dead instanceof Pillager) {
            Entity vehicle = dead.getVehicle();
            if (vehicle instanceof Snowman s && s.getPersistentDataContainer().has(leashKey, PersistentDataType.STRING)) {
                for (Entity passenger : s.getPassengers()) passenger.leaveVehicle();
                s.remove();
            }
        }
    }
}
