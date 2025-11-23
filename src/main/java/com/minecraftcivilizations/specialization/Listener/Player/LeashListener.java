package com.minecraftcivilizations.specialization.Listener.Player;

import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillLevel;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.Specialization;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowman;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;

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


        if (target.getVehicle() instanceof Snowman proxy) {
            if (proxy.getPersistentDataContainer().has(leashKey, PersistentDataType.BOOLEAN)) {

                // --- Drop a lead on the ground at the proxy location ---
                proxy.getWorld().dropItemNaturally(
                        proxy.getLocation(),
                        new ItemStack(Material.LEAD, 1)
                );


                if (target instanceof Player targetPlayer) {
                    // Clear their carried-mount state
                    if (downedListener.isDowned(targetPlayer)) {
                    downedListener.clearMount(targetPlayer);
                        downedListener.setSit(targetPlayer);
                    }
                    return;
                }
            }
        }


        // Must hold lead in either hand
        if (leasher.getInventory().getItemInMainHand().getType() != Material.LEAD &&
                leasher.getInventory().getItemInOffHand().getType() != Material.LEAD) return;

        // Normal right-click: only leash
        if (target.getVehicle() instanceof Snowman) return; //must not already be leashed or carried
//                leasher.sendMessage("Hello");

        if (isPlayerTarget) {
            Player targetPlayer = (Player) target;
            CustomPlayer h = CoreUtil.getPlayer(leasher);

            if (h.getSkillLevel(SkillType.GUARDSMAN) < SkillLevel.GRANDMASTER.getLevel()) {
                leasher.sendMessage("§0[§0§6CivLabs§0]§8 » §7You are not strong enough for that");
                return;
            }

            if (!downedListener.isDowned(targetPlayer)) {
                leasher.sendMessage("§0[§0§6CivLabs§0]§8 » §7You are not able to keep them still enough for that");
                return;
            }


            downedListener.clearMount(targetPlayer);
            Snowman proxy = spawnProxy(targetPlayer.getLocation(), leasher);
            proxy.addPassenger(targetPlayer);
            proxy.setLeashHolder(leasher);
            proxy.getPersistentDataContainer().set(leashKey, PersistentDataType.BOOLEAN, true);
            proxies.put(targetPlayer, proxy);
            leasher.getInventory().getItemInMainHand().subtract(1);
            e.setCancelled(true);

        }
    }


    // -------------------------
    // Relog handling
    // -------------------------
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
//        Player p = e.getPlayer();
////        downedListener.clearMount(p);
//        Snowman proxy = proxies.get(p);
//        if (proxy != null && proxy.isValid()) {
//            proxy.addPassenger(p);
//        }
    }

//    @EventHandler
//    public void onQuit(PlayerQuitEvent e) {
//        Player p = e.getPlayer();
//        Snowman proxy = proxies.get(p);
//        if (proxy != null) {
//            if (downedListener.isDowned(p)) {
//                downedListener.setSit(p);
//            }
//            proxy.remove();
//            proxies.remove(p);
//        }
//    }

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
        Location spawnLoc = loc.clone().add(0, 0.5, 0);
        Snowman s = spawnLoc.getWorld().spawn(spawnLoc, Snowman.class, sm -> {
            sm.setSilent(true);
            sm.setInvisible(true);
            sm.setAware(false);
            sm.setAI(true);
            sm.setPersistent(true);
            sm.setRemoveWhenFarAway(false);
            sm.setGravity(true);
            sm.setInvulnerable(true);
            sm.setCanPickupItems(false);
            sm.setCollidable(false);
            sm.setGlowing(false);
            sm.getAttribute(Attribute.STEP_HEIGHT).setBaseValue(1.0);
            sm.getAttribute(Attribute.SCALE).setBaseValue(0.13);
            sm.getEquipment().clear();

        });
        return s;
    }

    public void removeProxy(Player player) {
        Snowman proxy = proxies.get(player);
        if (proxy != null && proxy.isValid()) {
            for (Entity passenger : proxy.getPassengers()) passenger.leaveVehicle();
            proxy.remove();
            proxies.remove(player);

        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        Entity dead = e.getEntity();
        // If a player dies and has a tracked snowman
        if (dead instanceof Player p) {
            Snowman proxy = proxies.get(p);
            if (proxy != null && proxy.isValid()) {
                removeProxy(p);
                p.getWorld().dropItemNaturally(
                        p.getLocation(),
                        new ItemStack(Material.LEAD, 1)
                );
            }
        }
    }
}
