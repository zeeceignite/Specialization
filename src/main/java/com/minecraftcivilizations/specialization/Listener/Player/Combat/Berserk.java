package com.minecraftcivilizations.specialization.Listener.Player.Combat;

import com.google.gson.reflect.TypeToken;
import com.minecraftcivilizations.specialization.Config.PotionEffectData;
import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import lombok.NonNull;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;

public class Berserk implements Listener {

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent e) {
        // Check if the damaged entity is a player first
        if (e.getEntity().getType() != EntityType.PLAYER) {
            return;
        }

        Player player = (Player) e.getEntity();
        CustomPlayer damaged = CoreUtil.getPlayer(player);

        if (damaged.getSkillLevel(SkillType.GUARDSMAN) >= 1) {
            if (player.getHealth() - e.getFinalDamage() <= 3) {
                showMyTitleWithDurations(player);
                applyBerserk(player);
            }
        }
    }

    public void showMyTitleWithDurations(final @NonNull Audience target) {
        final Title.Times times = Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(1000));
        final Title title = Title.title(Component.text("Awakened"), Component.text("Muscle memory floods back from battles never fought."), times);

        target.showTitle(title);
    }

    public void applyBerserk(Player player) {
        for (PotionEffectType potionEffectType : Registry.MOB_EFFECT) {
            try {
                NamespacedKey effectKey = potionEffectType.getKey();
                PotionEffectData effectData = SpecializationConfig.getBerserkConfig().get(effectKey, new TypeToken<>() {});

                if (effectData != null && effectData.amplifier() > 0 && effectData.duration() > 0) {
                    player.addPotionEffect(new PotionEffect(potionEffectType, effectData.duration(), effectData.amplifier(), false, false));
                }

            } catch (Exception e) {
                System.err.println("Failed to apply berserk effect " + potionEffectType.getKey() + ": " + e.getMessage());
            }
        }
    }
}