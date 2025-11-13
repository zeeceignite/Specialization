package com.minecraftcivilizations.specialization.MobGoals;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.Specialization;
import com.minecraftcivilizations.specialization.StaffTools.Debug;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.*;

import java.util.EnumSet;
import java.util.function.Predicate;

public class TargetPlayerMobGoal implements Goal<Mob> {
    public static final GoalKey<Mob> KEY = GoalKey.of(Mob.class, new NamespacedKey(Specialization.getInstance(),"monster_target_player"));
    private Mob mob; //the mob of this goal

    public TargetPlayerMobGoal(Mob mob){
        this.mob = mob;
    }

    @Override
    public boolean shouldActivate() {
        return true;
//        return monster.getTargecull && !m**World().isDayTime() && !(monster instanceof Enderman) && !(monster instanceof Piglin);
    }

    @Override
    public void start() {
        calculateNewTarget();
    }

    public void calculateNewTarget(){
        int targetRange = SpecializationConfig.getMobConfig().get("MOB_RULE_TARGET_RANGE", Integer.class);
        Predicate<Player> validGamemode = p ->
                p.getGameMode() == GameMode.SURVIVAL ||
                        p.getGameMode() == GameMode.ADVENTURE;

        mob.getLocation().getNearbyPlayers(targetRange).stream()
                .filter(validGamemode)
                .filter(p -> p.getLocation().distance(mob.getLocation())<64)
                .min((p1, p2) -> {
                    CustomPlayer player1 = CoreUtil.getPlayer(p1);
                    CustomPlayer player2 = CoreUtil.getPlayer(p2);
                    // First, check the priority
                    if (player1.getSkillLevel(SkillType.GUARDSMAN) > player2.getSkillLevel(SkillType.GUARDSMAN)) {
                        return -1; // p1 has priority, so it comes first
                    }
                    if (player1.getSkillLevel(SkillType.GUARDSMAN) < player2.getSkillLevel(SkillType.GUARDSMAN)) {
                        return 1;  // p2 has priority, so it comes first
                    }
                    // If priorities are the same, compare by distance
                    if(mob.getLocation().getWorld().equals(p1.getLocation().getWorld()) && mob.getLocation().getWorld().equals(p2.getLocation().getWorld())){
                        return Double.compare(p1.getLocation().distanceSquared(mob.getLocation()), p2.getLocation().distanceSquared(mob.getLocation()));
                    }
                    return 0;
                })
                .ifPresent(player -> mob.setTarget(player));


        if(mob.getTarget()!=null) {
            Debug.broadcast("mobgoal", mob.getType().name().toLowerCase() + ": <red>targeting player" + mob.getTarget().getName());
        }
    }

    @Override
    public void tick() {
        Goal.super.tick();
        if(mob.getTarget() == null){
            calculateNewTarget(); //ensures the mob always has a new target
        }
    }

    @Override
    public GoalKey<Mob> getKey() {
        return KEY;
    }

    @Override
    public EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.TARGET, GoalType.MOVE);
    }
}
