package com.minecraftcivilizations.specialization.Combat.Mobs;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import com.google.gson.reflect.TypeToken;
import com.minecraftcivilizations.specialization.Combat.Instinct;
import com.minecraftcivilizations.specialization.Reinforcement.ReinforcementManager;
import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.Specialization;
import com.minecraftcivilizations.specialization.StaffTools.Debug;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import com.minecraftcivilizations.specialization.util.MathUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.bukkit.Material.*;

public class HuntPlayerMobGoal implements Goal<Mob> {
    public static final GoalKey<Mob> KEY = GoalKey.of(Mob.class, new NamespacedKey(Specialization.getInstance(), "monster_hunt_player"));

    private final Mob mob;
    private final double follow_range;
    private final boolean breaks_blocks;

    private float breakAmount = 0f;
    private Block block;
    private Collection<Player> nearbyPlayers;
    private static final Random random = new Random();

    private Entity last_target = null;
    private int tick = 0;
    private int reacquire_tick = 0;
    private float break_scalar;

    private static final EnumSet<Material> deniedTypes = EnumSet.of(OBSIDIAN, BEDROCK);

    public HuntPlayerMobGoal(Mob mob, double follow_range, boolean breaks_blocks, double break_scalar) {
        this.mob = mob;
        this.follow_range = follow_range;
        this.breaks_blocks = breaks_blocks;
        this.break_scalar = (float)break_scalar;
    }

    @Override
    public boolean shouldActivate() {
        boolean b = mob.getTarget() == null || (breaks_blocks && mob.getTarget() != null);
        // Activate if we need to search for a target, or if we have a target and this goal should manage breaking
        Debug.broadcast("huntplayer", "should activate: "+(b?"<green>TRUE":"<red>FALSE"));
        return b;
    }

    @Override
    public boolean shouldStayActive() {
        // Stay active while searching or while we have a target (so tick() can handle both acquiring and breaking)
        return true;
    }

    @Override
    public void start() {
        Debug.broadcast("huntplayer", "<green>starting hunt player for " + mob.getName());
        // immediate attempt to find a target if none exists
        if (mob.getTarget() == null) {
            calculateNewTarget();
        } else {
            // ensure internal state reflects current target
            last_target = mob.getTarget();
            breakAmount = 0f;
            block = null;
            nearbyPlayers = null;
        }
        tick = 0;
        reacquire_tick = 0;
    }

    public void calculateNewTarget() {
        Predicate<Player> validGamemode = p ->
                p.getGameMode() == GameMode.SURVIVAL ||
                        p.getGameMode() == GameMode.ADVENTURE;

        mob.getLocation().getNearbyPlayers(follow_range).stream()
                .filter(validGamemode)
                .filter(p -> p.getLocation().distance(mob.getLocation()) < 48)
                .min((p1, p2) -> {
                    CustomPlayer player1 = CoreUtil.getPlayer(p1);
                    CustomPlayer player2 = CoreUtil.getPlayer(p2);
                    if (player1.getSkillLevel(SkillType.GUARDSMAN) > player2.getSkillLevel(SkillType.GUARDSMAN)) return -1;
                    if (player1.getSkillLevel(SkillType.GUARDSMAN) < player2.getSkillLevel(SkillType.GUARDSMAN)) return 1;
                    if (mob.getLocation().getWorld().equals(p1.getLocation().getWorld()) && mob.getLocation().getWorld().equals(p2.getLocation().getWorld())) {
                        return Double.compare(p1.getLocation().distanceSquared(mob.getLocation()), p2.getLocation().distanceSquared(mob.getLocation()));
                    }
                    return 0;
                })
                .ifPresent(player -> mob.setTarget(player));

        if (mob.getTarget() != null) {
            Debug.broadcast("huntplayer", mob.getType().name().toLowerCase() + ": <red>targeting player</red> " + mob.getTarget().getName());
        }
    }

    @Override
    public void tick() {
        // If we don't have a target, periodically try to acquire one



        tick++;
        if (tick % 20 == 0) { // once per second
            if (mob.getTarget() == null) {
                    calculateNewTarget();
                return;
            }
            tick = 0;
        }

        // We have a target. detect target changes and reset state
        Entity current_target = mob.getTarget();
        if(current_target==null){
            return;
        }else if (current_target != last_target) {
            last_target = current_target;
            breakAmount = 0f;
            block = null;
            nearbyPlayers = null;
            // small immediate raytrace attempt next tick
            reacquire_tick = 0;
        }

        // If this mob doesn't break blocks, nothing more to do here (movement/pathing handled by other systems)
        if (!breaks_blocks) return;

        // Do not attempt breaking during daytime
        if (mob.getWorld().isDayTime()) {
            block = null;
            breakAmount = 0f;
            nearbyPlayers = null;
            return;
        }

        // Occasional chance check before attempting to break anything.
        // Only run this check when we don't currently have a candidate block.
        if (block == null) {
            double percentage = SpecializationConfig.getMobConfig().get("BLOCK_BREAK_CHANCE_PERCENTAGE", Double.class);
            if (random.nextDouble() > percentage / 100d) {
                // Skip breaking attempt this cycle; try again later (every 20 ticks)
                reacquire_tick++;
                if (reacquire_tick < 20) return;
                reacquire_tick = 0;
            }

            // Basic validation: same world and within configured target radius
            if (!mob.getWorld().equals(current_target.getWorld())) return;

            Vector vectorToPlayer = current_target.getLocation().subtract(mob.getEyeLocation().add(0,1,0)).toVector().normalize().add(MathUtils.randomVectorCentered(0.25)).normalize();
            // Raytrace for a blocking block up to distance 5 from the mob's eye (like the previous logic)
            RayTraceResult result = mob.getWorld().rayTrace(mob.getEyeLocation(), vectorToPlayer.normalize(), 3, FluidCollisionMode.NEVER, true, .15, entity -> false);
            if (result == null || result.getHitBlock() == null) {
                Location leglocation = mob.getLocation().add(0,0.5,0);
                vectorToPlayer = current_target.getLocation().subtract(leglocation).toVector();
                result = mob.getWorld().rayTrace(leglocation, vectorToPlayer.normalize(), 3, FluidCollisionMode.NEVER, true, .15, entity -> false);
                if (result == null || result.getHitBlock() == null) {
//                    Debug.broadcast("huntplayer", "<#554400>Both blocks null");
                    return;
                }else{
//                    Debug.broadcast("huntplayer", "<gold>Block found on Floor location");
                }
            }else{
//                Debug.broadcast("huntplayer", "<gold>Block found on Eye location");
            }

            Block hit = result.getHitBlock();
            if (hit == null) return;

//            List<String> deniedBlocks = SpecializationConfig.getMobConfig().get("BLOCK_BREAK_IGNORE_LIST_REGEX", new TypeToken<>(){}); screw the config
            if (hit.getType() == Material.AIR) return;

            if(deniedTypes.contains(hit.getType())){
                return;
            }

            /**
             * Prevents mobs from breaking blocks at their feet
             */
            if(hit.getLocation().getY()<mob.getLocation().getY()+0.25){
                //block is below mob
                if(current_target.getLocation().getY()>=hit.getLocation().getY()){
                    return;
                }
            }

            // We have a valid block to break
            block = hit;
            breakAmount = 0f;
            nearbyPlayers = block.getLocation().getNearbyPlayers(16).stream()
                    .filter(player -> player.getGameMode().equals(GameMode.SURVIVAL))
                    .collect(Collectors.toSet());

            // Trigger instinct system (same behavior as previous BreakBlockMobGoal)
            Instinct.onMobStartBreakingBlock((Monster) mob);
        }

        // If we have a block, increment break progress and show visuals
        if (block != null) {

            /**
             * Block validation, incase it was broken
             * Also checks if the mob has walked too far away from the block
             */
            if (block.getType() == Material.AIR || block.getLocation().distanceSquared(mob.getLocation()) > 42) {
                if (nearbyPlayers != null && !nearbyPlayers.isEmpty()) {
                    nearbyPlayers.forEach(player -> player.sendBlockDamage(block.getLocation(), 0));
                }

                block = null;
                breakAmount = 0f;
                nearbyPlayers = null;
                return;
            }

            float breakPercentagePerTick = 2f; //SpecializationConfig.getMobConfig().get("VISUAL_BREAKING_INCREASE_PER_TICK_PERCENTAGE", Float.class);
            if (ReinforcementManager.isReinforced(block)){
                if(ReinforcementManager.isLightlyReinforced(block)){
                    breakPercentagePerTick = 0.25f;
                }
                if(ReinforcementManager.isHeavilyReinforced(block)){
                    breakPercentagePerTick = 0.125f;
                }
            }
            breakPercentagePerTick *= break_scalar;
            breakAmount += breakPercentagePerTick / 100f;

            if (breakAmount >= 1.0f) {
                // Only break "hard" blocks
                if (block.getBlockData().getMaterial().getHardness() > 0) {
                    block.breakNaturally(true, false);
                }
                // reset so we'll attempt to find a new obstruction next cycle
                block = null;
                breakAmount = 0f;
                nearbyPlayers = null;
                return;
            }

            // Send block damage to nearby players so they see progress
            if (nearbyPlayers != null && !nearbyPlayers.isEmpty()) {
                nearbyPlayers.forEach(player -> player.sendBlockDamage(block.getLocation(), breakAmount));
            }
        }
    }

    @Override
    public void stop() {
        Debug.broadcast("huntplayer", "<gray> stopping");
        // clean up internal state
        last_target = null;
        block = null;
        breakAmount = 0f;
        nearbyPlayers = null;
        Goal.super.stop();
    }

    @Override
    public GoalKey<Mob> getKey() {
        return KEY;
    }

    @Override
    public EnumSet<GoalType> getTypes() {
        // This goal both picks a target and manages movement/interaction (breaking)
        return EnumSet.of(GoalType.TARGET, GoalType.MOVE);
    }
}
