package com.minecraftcivilizations.specialization.Listener;

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
import java.util.Map;

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

                    for(PotionEffectType potionEffectType : PotionEffectType.values()) {
                        PotionData potionData = SpecializationConfig.getBerserkConfig().get(potionEffectType, PotionData.class);
                        if (potionData != null) {
                            PotionEffect effect = new PotionEffect(
                                    potionEffectType,
                                    (int)(potionData.dur() * 20),
                                    potionData.amp()
                            );
                            player.addPotionEffect(effect);
                        }
                    }
                }
            }
        }
    }
    public void showMyTitleWithDurations(final @NonNull Audience target) {
        final Title.Times times = Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(1000));
        final Title title = Title.title(Component.text("Awakened"), Component.text("Muscle memory floods back from battles never fought."), times);

        target.showTitle(title);
    }
}
