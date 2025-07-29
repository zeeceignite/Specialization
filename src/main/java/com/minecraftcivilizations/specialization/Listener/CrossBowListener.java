package com.minecraftcivilizations.specialization.Listener;

import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;

public class CrossBowListener implements Listener {


    @EventHandler
    public void onCrossBowShoot(EntityShootBowEvent bowEvent) {
        assert bowEvent.getBow() != null;
        Arrow arrow = (Arrow) bowEvent.getProjectile();


        if (arrow.isShotFromCrossbow()) {
            arrow.setVelocity(arrow.getVelocity().multiply(SpecializationConfig.getCombatConfig().getDouble("CROSSBOW_BASE_VELOCITY")));
            if (bowEvent.getBow().containsEnchantment(Enchantment.MULTISHOT)) {
                arrow.setVelocity(arrow.getVelocity().multiply(SpecializationConfig.getCombatConfig().getDouble("CROSSBOW_BASE_MULTISHOT_VELOCITY")));
            } else if (bowEvent.getBow().containsEnchantment(Enchantment.PIERCING)) {
                arrow.setVelocity(arrow.getVelocity().multiply(SpecializationConfig.getCombatConfig().getDouble("CROSSBOW_BASE_PIERCING_VELOCITY")));
            } else if (bowEvent.getBow().containsEnchantment(Enchantment.QUICK_CHARGE)) {
                arrow.setVelocity(arrow.getVelocity().multiply(SpecializationConfig.getCombatConfig().getDouble("CROSSBOW_BASE_QUICKCHARGE_VELOCITY")));
            }
        }
    }

}
