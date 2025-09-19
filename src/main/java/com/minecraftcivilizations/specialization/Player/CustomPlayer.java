package com.minecraftcivilizations.specialization.Player;

import com.google.common.collect.Queues;
import com.google.gson.reflect.TypeToken;
import com.minecraftcivilizations.specialization.Config.SpecializationConfig;
import com.minecraftcivilizations.specialization.Skill.Skill;
import com.minecraftcivilizations.specialization.Skill.SkillLevel;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.util.LoreUtils;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import minecraftcivilizations.com.minecraftCivilizationsCore.MinecraftCivilizationsCore;
import minecraftcivilizations.com.minecraftCivilizationsCore.Options.Pair;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static com.minecraftcivilizations.specialization.Skill.Skill.getXPNeededForLevel;
import static com.minecraftcivilizations.specialization.Skill.Skill.mapValue;
import static com.minecraftcivilizations.specialization.Skill.SkillType.getDisplayName;

@Getter
public class CustomPlayer extends minecraftcivilizations.com.minecraftCivilizationsCore.Player.CustomPlayer {
    @Getter
    @Setter
    private SkillType preferredSkill = SkillType.values()[ThreadLocalRandom.current().nextInt(SkillType.values().length)];
    @Getter
    List<Skill> skills = new ArrayList<>(0);
    @Setter
    @Getter
    private double height = 0;
    @Getter
    @Setter
    private boolean isAdvancedClassesGUIEnabled = false;
    @Getter
    @Setter
    private boolean isSoundEnabled = true;
    @Getter
    @Setter
    private boolean isNewRecipeGUIIteration = false;
    @Getter
    @Setter
    private AnalyticPlayerData analyticPlayerData = new AnalyticPlayerData();
    @Getter
    private boolean isDowned = false;
    @Getter
    @Setter
    private long lastDowned = System.currentTimeMillis();
    @Getter
    @Setter
    private boolean wasDownedOnLogout = false;
    @Getter
    private final List<NamespacedKey> additionUnlockedRecipes = new ArrayList<>();
    @Getter
    private final HashSet<UUID> leashedOtherPlayers = new HashSet<>();
    @Setter
    private UUID leashedTo = null;
    private final Queue<Material> lastEatenFood = Queues.newConcurrentLinkedQueue();

    public CustomPlayer(UUID uuid) {
        super(uuid);
        loadPlayer();

    }

    private void loadPlayer() {
        CustomPlayer customPlayer = (CustomPlayer) MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().load(this.getUuid());

        if (customPlayer != null) {
            return;
        }

        for (SkillType skill : SkillType.values()) {
            Skill skill1 = new Skill(skill, 0, System.currentTimeMillis());
            skill1.setSkillType(skill);
            this.skills.add(skill1);
        }

        Player player = Bukkit.getPlayer(getUuid());

        if (player == null) return;

        Objects.requireNonNull(player.getAttribute(Attribute.BLOCK_BREAK_SPEED)).setBaseValue(0);
        Objects.requireNonNull(player.getAttribute(Attribute.BLOCK_BREAK_SPEED)).setBaseValue(0);
    }



    public void addSkillXp(SkillType skillType, double xp) {
        if (skillType == null || xp == 0) return;
        int previousLevel = this.getSkillLevel(skillType);
        getSkill(skillType).xp(xp);
        Player player = Bukkit.getPlayer(getUuid());
        player.sendActionBar(Component.text((xp <= 0 ? "" : "+") + xp).color(NamedTextColor.WHITE).append(Component.text(" (" + getDisplayName(skillType) + ")").color(NamedTextColor.GRAY)));
        int currentLevel = this.getSkillLevel(skillType);

        if (this.isSoundEnabled) {
            player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 100, 1);
        }

        // Update team assignment based on highest skill
        TeamManager.setTeam(Bukkit.getPlayer(getUuid()));


        if (previousLevel != currentLevel) {
            applyEffects();
            if (previousLevel < currentLevel) {
                player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 100, 1);
                player.sendMessage(LoreUtils.createLoreLine("You have leveled up " + SkillType.getDisplayName(skillType) + ", you are now " + SkillType.getDisplayName(skillType) + " " + SkillLevel.getDisplayName(currentLevel), NamedTextColor.WHITE));
            } else {
                player.playSound(player, Sound.ITEM_BOTTLE_FILL_DRAGONBREATH, 100F, 1.5F);
                player.sendMessage(LoreUtils.createLoreLine("Your " + SkillType.getDisplayName(skillType) + "ing ability has deteriorated, you are now " + SkillType.getDisplayName(skillType) + " " + SkillLevel.getDisplayName(currentLevel), NamedTextColor.WHITE));
            }
            while (currentLevel > 0) {
                Set<NamespacedKey> recipes =
                        SpecializationConfig.getUnlockedRecipesConfig().get(skillType.name() + "_" + SkillLevel.getSkillLevelFromInt(currentLevel), new TypeToken<>(){});
                for (NamespacedKey entry : recipes) {
                    player.discoverRecipe(entry);
                }
                currentLevel--;
            }

        }
    }

    public void applyEffects(){
        Player player = Bukkit.getPlayer(getUuid());
        Arrays.stream(SkillType.values()).forEach(skill -> {
            List<Pair<NamespacedKey, Integer>> potions = SpecializationConfig.getClassSkillEffectsConfig().get(skill + "_" + getSkillLevelEnum(skill), new TypeToken<>(){});
            assert player != null;
            for(Pair<NamespacedKey, Integer> dataEffect : potions) {
                PotionEffectType potionEffectType = Registry.EFFECT.get(dataEffect.firstValue());
                if(dataEffect.secondValue() < 0) continue;
                if(potionEffectType == null) throw new IllegalStateException("invalid potion effect type in config" + dataEffect.firstValue());
                if(player.getActivePotionEffects().stream().anyMatch(effect -> effect.getType().equals(potionEffectType) && effect.getDuration() == -1)){
                    player.removePotionEffect(potionEffectType);
                }
                if(player.getActivePotionEffects().stream().noneMatch(effect -> effect.getType().equals(potionEffectType) && effect.getAmplifier() > dataEffect.secondValue())){
                    player.removePotionEffect(potionEffectType);
                    player.addPotionEffect(new PotionEffect(potionEffectType,-1, dataEffect.secondValue()));
                }
            }
        });
    }

    public int getSkillLevel(SkillType skillType) {
        int level;
        // So, so sorry if you have to read this, it was fixed about 10 times and I forgot to call it, so now it looks like this :sad:
        level = 0;
        while (level < SkillLevel.values().length && !isMissingXpForLevel(skillType, level+1) && !isMissingPercentForLevel(skillType, level+1)) {
            level++;
        }
        return level;
    }

    public SkillLevel getSkillLevelEnum(SkillType skillType) {
        return SkillLevel.getSkillLevelFromInt(getSkillLevel(skillType));
    }

    public double getTotalXp() {
        double totalXp = 0;
        for (Skill skill : this.skills) {
            totalXp += skill.getXp();
        }
        return totalXp;
    }

    private boolean isMissingXpForLevel(SkillType skillType, int level) {
        return getSkill(skillType).getXp() < getXPNeededForLevel(level);
    }

    private boolean isMissingPercentForLevel(SkillType skillType, int level) {
        return getPercentOfTotal(skillType) < SpecializationConfig.getSkillRequirementsConfig().get(skillType + "_" + SkillLevel.getSkillLevelFromInt(level) + "_REQUIREMENT", Double.class);
    }

    public double getGUIDistributionOfTotalSkills(SkillType skillType) {
        return mapValue(getSkill(skillType).getXp(), 0, getTotalXp(), 0, 3);
    }

    public double getGUIDistributionOfTotalLevels(SkillType skillType) {
        int level = getSkillLevel(skillType);
        double XPMin = level == 0 ? 0 : getXPNeededForLevel(level);
        double XPMax = getXPNeededForLevel(level + 1);

        Skill skill = getSkill(skillType);
        double percentageNeededMin = SpecializationConfig.getSkillRequirementsConfig().get(skill.getSkillType() + "_" + SkillLevel.getSkillLevelFromInt(level) + "_REQUIREMENT", Double.class);
        double percentageNeededMax = SpecializationConfig.getSkillRequirementsConfig().get(skill.getSkillType() + "_" + SkillLevel.getSkillLevelFromInt(level + 1) + "_REQUIREMENT", Double.class);
        double currentPercentage = getPercentOfTotal(skillType);

        double XPProgressAsPercentage;
        if(skill.getXp() <= XPMax) {
            XPProgressAsPercentage = mapValue(skill.getXp() - XPMin, 0, XPMax - XPMin, 0, 100);
        }else XPProgressAsPercentage = 100.0;

        double percentageProgressAsPercentage;

        if(currentPercentage <= percentageNeededMax) {
            percentageProgressAsPercentage = mapValue(currentPercentage - percentageNeededMin, 0, percentageNeededMax - percentageNeededMin,0,100);
        }else if(getTotalXp() == 0){
            percentageProgressAsPercentage = 0;
        } else percentageProgressAsPercentage = 100.0;
        return Math.round(XPProgressAsPercentage * percentageProgressAsPercentage * .01);
    }

    public boolean isDownedTimeout() {
        if (isDowned) {
            return lastDowned + 80 <= System.currentTimeMillis();
        }
        return false;
    }

    public double getPercentOfTotal(SkillType skillType) {
        return mapValue(getSkill(skillType).getXp(), 0, getTotalXp(), 0, 100);
    }

    public void setDowned(boolean downed) {
        if (this.isDowned != downed) {
            this.isDowned = downed;
            if (!downed) return;
            lastDowned = System.currentTimeMillis();
            new BukkitRunnable() {
                final double totalTime = SpecializationConfig.getDownedConfig().get("TIME_TO_DEATH_IN_TICKS", Double.class);
                double currentTime = 0;
                @Override
                public void run() {
                    if (!isDowned) {
                        this.cancel();
                        return;
                    }
                    if (currentTime >= totalTime && CustomPlayer.this.isDowned()) {
                        Bukkit.getPlayer(CustomPlayer.this.getUuid()).setHealth(0);
                    }
                    currentTime ++;
                }
            }.runTaskTimer(MinecraftCivilizationsCore.getInstance(), 0, 1);
        }
    }

    public Skill getSkill(SkillType skillType) {
        for (Skill skill : this.skills) {
            if (skill.getSkillType() == skillType) {
                return skill;
            }
        }
        throw new IllegalStateException("Couldn't get skill " + skillType.toString());
    }

    @Data
    public static class AnalyticPlayerData {
        int deaths; // Total cumulative deaths (kept for backward compatibility)
        int deathsThisPeriod; // Deaths in current 5-minute period

        public void incrementDeathsThisPeriod() {
            this.deathsThisPeriod++;
            this.deaths++; // Also increment total for backward compatibility
        }

        public void resetDeathsForPeriod() {
            this.deathsThisPeriod = 0;
        }
    }

    /**
        returns true if the food hasn't been eaten in the last 5 foods, false otherwise
     */
    public boolean eatFood(Material food){
        boolean result = !lastEatenFood.contains(food);
        lastEatenFood.add(food);
        if(lastEatenFood.size() > 5){
            lastEatenFood.poll();
        }
        return result;
    }
}
