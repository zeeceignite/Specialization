package com.minecraftcivilizations.specialization.Listener.Player.Combat;

import com.minecraftcivilizations.specialization.StaffTools.Debug;
import com.minecraftcivilizations.specialization.util.MathUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/**
 * Raytraced armor
 * @author alectriciti
 */
public class DynamicArmor {

    CombatManager combatManager;

    public DynamicArmor(CombatManager combatManager){
        this.combatManager = combatManager;
    }

    void applyRaytracedArmorHit(EntityDamageByEntityEvent event) {
        if(!(event.getDamager() instanceof Player player))return;
        if(!(event.getEntity() instanceof HumanEntity victim))return;


        double armor_damage = event.getDamage(EntityDamageEvent.DamageModifier.ARMOR); //cache the armor damage value
        Vector direction = MathUtils.getDirectionVector(player.getYaw(), player.getPitch());
        Location origin = player.getEyeLocation();

        EquipmentSlot slot = getRaytracedCollisionWithEntity(origin, victim, direction, 0.03, 5);
        if(slot!=null){
            ItemStack equipped = null;
            switch (slot) {
                case HEAD -> {
                    equipped = victim.getEquipment().getHelmet();
                    Debug.broadcast("armor", "Head Hit!");
                }
                case CHEST -> {
                    equipped = victim.getEquipment().getChestplate();
                    Debug.broadcast("armor", "Chest Hit!");
                }
                case LEGS -> {
                    equipped = victim.getEquipment().getLeggings();
                    Debug.broadcast("armor", "Legs Hit!");
                }
                case FEET -> {
                    equipped = victim.getEquipment().getBoots();
                    Debug.broadcast("armor", "Feet Hit!");
                }
                default -> equipped = null;
            }
            if (equipped != null && equipped.getType().isAir() == false) {
                // That region is protected. restore full vanilla armor contribution.
                event.setDamage(EntityDamageEvent.DamageModifier.ARMOR, armor_damage);
            } else {
                // region unprotected: leave armor modifier at zero -> full damage passes through
                event.setDamage(EntityDamageEvent.DamageModifier.ARMOR, 0d);
            }
        }


        //if raytrace lands at different parts of the body
//        event.setDamage(EntityDamageEvent.DamageModifier.ARMOR, 0);
    }



    // tune these values to taste
    private static final double DEFAULT_MAX_DISTANCE = 5.0; // how far the ray goes
    private static final double DEFAULT_STEP = 0.08; // step length per iteration
    private static final double HIT_RADIUS_XZ = 0.5; // horizontal hit radius for player body

    /**
     * this will perform a raytrace until it hits a target entity
     * @param target this is the target we collide with. the goal is to get a certain part of their body
     */
    /**
     * Raytrace along trajectory from start; return the EquipmentSlot corresponding to hit region,
     * or null when no hit found.
     *
     * Simple geometric classification by Y-level:
     *  - HEAD: top portion near eye level
     *  - CHEST: upper-mid torso
     *  - LEGS: lower torso
     *  - FEET: near the ground
     *
     * This avoids depending on NMS bounding boxes and works on Spigot/Paper.
     */
    private EquipmentSlot getRaytracedCollisionWithEntity(Location start,
                                                          HumanEntity target,
                                                          Vector trajectory,
                                                          double step,
                                                          double maxDistance) {
        Vector dir = trajectory.clone().normalize();

        Location feetLoc = target.getLocation();
        double feetY = feetLoc.getY();
        double eyeY = target.getEyeLocation().getY();
        double fullHeight = eyeY - feetY + 0.2; // small cushion

        // thresholds relative to feetY (these can be tuned)
        double headMinY = eyeY - 0.25;          // anything >= this is head region
        double chestMinY = eyeY - 0.9;          // between chestMinY..headMinY is chest
        double legsMinY = feetY + 0.55;         // between legsMinY..chestMinY is legs
        double feetMaxY = feetY + 0.55;         // <= this is feet

        // central XZ of target (use location center)
        double targetX = feetLoc.getX();
        double targetZ = feetLoc.getZ();

        // iterate along the ray
        double traveled = 0d;
        while (traveled <= maxDistance) {
            Vector sample = dir.clone().multiply(traveled).add(start.toVector());
            double sx = sample.getX();
            double sy = sample.getY();
            double sz = sample.getZ();

            // simple XZ proximity test
            double dx = sx - targetX;
            double dz = sz - targetZ;
            double distXZ2 = dx * dx + dz * dz;
            if (distXZ2 <= HIT_RADIUS_XZ * HIT_RADIUS_XZ) {
                // check vertical overlap within the player's body height
                if (sy >= feetY - 0.1 && sy <= feetY + fullHeight + 0.1) {
                    // classify by Y
                    if (sy >= headMinY) return EquipmentSlot.HEAD;
                    if (sy >= chestMinY) return EquipmentSlot.CHEST;
                    if (sy >= legsMinY) return EquipmentSlot.LEGS;
                    if (sy <= feetMaxY) return EquipmentSlot.FEET;
                    // fallback to chest if ambiguous
                    feetLoc.getWorld().spawnParticle(Particle.FLAME, new Location(target.getWorld(), sx, sy, sz), 10, 0.01,0.01,0.01);
                    return EquipmentSlot.CHEST;
                }
            }

            traveled += step;
        }

        return null;
    }

}
