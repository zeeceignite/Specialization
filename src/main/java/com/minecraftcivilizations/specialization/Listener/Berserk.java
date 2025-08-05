package com.minecraftcivilizations.specialization.Listener;

import com.comphenix.protocol.wrappers.Pair;
import com.google.gson.reflect.TypeToken;
import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import lombok.NonNull;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
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
       EntityType type = e.getEntity().getType();
       Player player = (Player) e.getEntity();
       CustomPlayer damaged = CoreUtil.getPlayer(player);

        if (type.equals(EntityType.PLAYER)) {
            if (damaged.getSkillLevel(SkillType.GUARDSMAN) >= 1) {
                if(player.getHealth() - e.getFinalDamage() <= 3) {
                    showMyTitleWithDurations(player);

                }
            }
        }
    }
    public void showMyTitleWithDurations(final @NonNull Audience target) {
        final Title.Times times = Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(1000));
        final Title title = Title.title(Component.text("Awakened"), Component.text("Muscle memory floods back from battles never fought."), times);

        target.showTitle(title);
    }
    public void applyBerserk(Player player) {
        for (PotionEffectType potionEffectType : PotionEffectType.values()) {
            Pair<Double, Double> effectData = SpecializationConfig.getBerserkConfig().get(potionEffectType, new TypeToken<>(){});

            double amp = effectData.getFirst();
            double dur = effectData.getSecond();

            player.addPotionEffect(new PotionEffect(potionEffectType, (int) amp,(int) dur));
        }
    }
}
