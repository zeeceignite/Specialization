package com.minecraftcivilizations.specialization.Mobs;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import com.minecraftcivilizations.specialization.Specialization;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Monster;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.EnumSet;

public class TargetPlayerMobGoal implements Goal<@NotNull Monster> {
    public static final GoalKey<@NotNull Monster> KEY = GoalKey.of(Monster.class, new NamespacedKey(Specialization.getInstance(),"monster_target_player"));
    private Monster monster;

    public TargetPlayerMobGoal(Monster monster){
        this.monster = monster;
    }

    @Override
    public boolean shouldActivate() {
        return monster.getTarget() == null;
    }

    @Override
    public void start() {
        monster.getLocation().getNearbyPlayers(64).stream()
                .min(Comparator.comparingDouble(p -> monster.getLocation().distance(p.getLocation())))
                .ifPresent(player -> monster.setTarget(player));
        if (monster.getWorld().isDayTime()) {

        }
    }

    @Override
    public @NotNull GoalKey<@NotNull Monster> getKey() {
        return KEY;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.TARGET, GoalType.MOVE);
    }
}
