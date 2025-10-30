package com.minecraftcivilizations.specialization.Listener.Player.Combat;

import com.google.gson.reflect.TypeToken;
import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Specialization;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
        import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;

import java.util.List;

/**
 * The parent manager for everything related to combat, including mob damage
 * Routes damage listeners
 */
public class CombatManager implements Listener {


    private final GuardsmanDamage guardsmanDamage;
    private final DynamicArmor dynamicArmor;
    private final ArmorDamageReduction armorDamageReduction; // Handles MOB -> PLAYER damage
    private final Berserk berserk; // Berserk Manager

    final Specialization plugin;

    public CombatManager(Specialization specialization) {
        this.plugin = specialization;
        specialization.getServer().getPluginManager().registerEvents(this, specialization);
        guardsmanDamage = new GuardsmanDamage(this);
        dynamicArmor = new DynamicArmor(this);
        armorDamageReduction = new ArmorDamageReduction(this);
        berserk = new Berserk(this);
    }

    @EventHandler
    public void GlobalDamageListener(EntityDamageByEntityEvent event) {
        // Check if the damager is a player


        if (event.getDamager() instanceof Player) {
            //Player related damage
            guardsmanDamage.applyGuardsmanDamage(event);
        } else {
            //Mob related armor reduction
            armorDamageReduction.applyArmorReduction(event);
        }
        dynamicArmor.applyRaytracedArmorHit(event);
    }

    /**
     * Temporary max health for mobs
     */
    @EventHandler
    public void onMobSpawn(CreatureSpawnEvent event){
        AttributeInstance attribute = event.getEntity().getAttribute(Attribute.MAX_HEALTH);
        double max_health = attribute.getValue()*2;
        attribute.setBaseValue(max_health);
        event.getEntity().setHealth(max_health);
    }

    /**
     * Custom Mob Drops
     */
    @EventHandler
    public void addCustomMobDrops(EntityDeathEvent e){
        if (e.getEntity().getKiller() != null) {
            Player player = e.getEntity().getKiller();
            assert player != null;
//            CustomPlayer killer = CoreUtil.getPlayer(e.getEntity().getKiller().getUniqueId());
//            EntityType entity = e.getEntity().getType();
            List<NamespacedKey> items = SpecializationConfig.getMobDropsConfig().get(e.getEntityType(), new TypeToken<>() {});
            Material.matchMaterial(e.getEntityType().getKey().getKey());
        }
    }

}