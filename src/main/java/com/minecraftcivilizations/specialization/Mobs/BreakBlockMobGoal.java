package com.minecraftcivilizations.specialization.Mobs;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Specialization;
import lombok.Getter;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Random;

public class BreakBlockMobGoal implements Goal<@NotNull Monster> {

    @Getter
    public static final GoalKey<@NotNull Monster> KEY = GoalKey.of(Monster.class, new NamespacedKey(Specialization.getInstance(),"monster_break_block"));

    private final Monster monster;
    private float breakAmount = 0;
    private Block block;
    private Collection<Player> nearbyPlayers;
    private static final Random random = new Random();

    public BreakBlockMobGoal(Monster monster) {
        // The constructor takes the Player to follow and the Camel that is following
        this.monster = monster;
    }


    @Override
    public boolean shouldActivate() {
        if(monster.getTarget() == null) return false;
        double percentage = SpecializationConfig.getMobConfig().get("BLOCK_BREAK_CHANCE_PERCENTAGE", Double.class);
        if(random.nextDouble() > percentage / 100d) return false;
        Vector directionToPlayer = monster.getTarget().getLocation().subtract(monster.getLocation()).toVector().normalize();
        RayTraceResult result = monster.getWorld().rayTraceBlocks(monster.getLocation(), directionToPlayer, 5);
        if(result == null || result.getHitBlock() == null) return false;
        block = result.getHitBlock();

        return true;
    }

    @Override
    public boolean shouldStayActive() {
        return breakAmount < 1.0;
    }

    @Override
    public void start() {
        nearbyPlayers = block.getLocation().getNearbyPlayers(16);
    }

    @Override
    public void tick() {
        float breakPercentagePerTick = SpecializationConfig.getMobConfig().get("VISUAL_BREAKING_INCREASE_PER_TICK_PERCENTAGE", Float.class);
        breakAmount += breakPercentagePerTick / 100f;
        if(breakAmount >= 1.0){
            block.breakNaturally();
            return;
        }

        nearbyPlayers.forEach(player -> player.sendBlockDamage(block.getLocation(), breakAmount));
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
