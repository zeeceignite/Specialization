package com.minecraftcivilizations.specialization.Player;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.minecraftcivilizations.specialization.Config.Config;
import com.minecraftcivilizations.specialization.Recipe.Pair;
import com.minecraftcivilizations.specialization.Skill.Skill;
import com.minecraftcivilizations.specialization.Skill.SkillLevel;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.Specialization;
import lombok.Getter;
import lombok.Setter;
import minecraftcivilizations.com.minecraftCivilizationsCore.MinecraftCivilizationsCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Recipe;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static com.minecraftcivilizations.specialization.Skill.Skill.getXPNeededForLevel;
import static com.minecraftcivilizations.specialization.Skill.Skill.mapValue;
import static com.minecraftcivilizations.specialization.Skill.SkillType.getDisplayName;

public class CustomPlayer extends minecraftcivilizations.com.minecraftCivilizationsCore.Player.CustomPlayer {
    @Getter
    @Setter
    private SkillType preferredSkill = SkillType.values()[ThreadLocalRandom.current().nextInt(SkillType.values().length)];
    @Getter
    List<Skill> skills = new ArrayList<>(0);
    @Setter
    @Getter
    private double height = 0;

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

        for (Iterator<Recipe> it = Bukkit.recipeIterator(); it.hasNext(); ) {
            Recipe recipe = it.next();
            if (recipe instanceof Keyed keyed) {
                player.undiscoverRecipe(keyed.getKey());
            }
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                Set<Pair> recipes = new Gson().fromJson(
                        Config.getDefaultUnlockedRecipesConfig().getString("DEFAULT_UNLOCKED_RECIPES"),
                        new TypeToken<Set<Pair>>() {
                        }.getType());
                for (Pair entry : recipes) {
                    NamespacedKey key = new NamespacedKey(entry.key(), entry.value());
                    player.discoverRecipe(key);
                }
            }
        }.runTaskLater(Specialization.getInstance(), 1);

        Objects.requireNonNull(player.getAttribute(Attribute.MINING_EFFICIENCY)).setBaseValue(0);
        Objects.requireNonNull(player.getAttribute(Attribute.BLOCK_BREAK_SPEED)).setBaseValue(0);
    }

    public void addSkillXp(SkillType skillType, double xp) {
        if (skillType == null) return;
        int previousLevel = this.getSkillLevel(skillType);
        getSkill(skillType).addXp(xp);
        Bukkit.getPlayer(getUuid()).sendActionBar(Component.text("+" + xp).color(NamedTextColor.WHITE).append(Component.text(" (" + getDisplayName(skillType) + ")").color(NamedTextColor.GRAY)));
        int currentLevel = this.getSkillLevel(skillType);

        if (previousLevel != currentLevel) {
            while (currentLevel > 0) {
                Set<Pair> recipes = new Gson().fromJson(
                        Config.getUnlockedRecipesConfig().getString(skillType.name() + "_" + SkillLevel.getSkillLevelFromInt(currentLevel)),
                        new TypeToken<Set<Pair>>() {}.getType());
                for (Pair entry : recipes) {
                    NamespacedKey key = new NamespacedKey(entry.key(), entry.value());
                    Bukkit.getPlayer(this.getUuid()).discoverRecipe(key);
                }

                currentLevel--;
            }
        }
    }

    public int getSkillLevel(SkillType skillType) {
        int level;
        // So, so sorry if you have to read this, it was fixed about 10 times and I forgot to call it, so now it looks like this :sad:
        for(level = 0; level < SkillLevel.values().length && !isMissingXpForLevel(skillType, level+1) && !isMissingPercentForLevel(skillType, level+1); level++);
        return level;
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
        return getPercentOfTotal(skillType) < Config.getSkillRequirementsConfig().getDouble(skillType.name() + "_" + SkillLevel.getSkillLevelFromInt(level) + "_REQUIREMENT");
    }

    public double getGUIDistributionOfTotalSkills(SkillType skillType, Player player) {
        int level = getSkillLevel(skillType);
        double XPMin = level == 0 ? 0 : getXPNeededForLevel(level);
        double XPMax = getXPNeededForLevel(level + 1);

        Skill skill = getSkill(skillType);
        double percentageNeededMin = Config.getSkillRequirementsConfig().getDouble(skill.getSkillType().name() + "_" + SkillLevel.getSkillLevelFromInt(level).name() + "_REQUIREMENT");
        double percentageNeededMax = Config.getSkillRequirementsConfig().getDouble(skill.getSkillType().name() + "_" + SkillLevel.getSkillLevelFromInt(level + 1).name() + "_REQUIREMENT");
        double currentPercentage = getPercentOfTotal(skillType);

        double XPProgressAsPercentage;
        player.sendMessage("xpmin:" + XPMin + " and max" + XPMax);
        player.sendMessage("current percentage:" + currentPercentage + " min" + percentageNeededMin + " max" + percentageNeededMax);
        if(skill.getXp() <= XPMax) {
            XPProgressAsPercentage = mapValue(skill.getXp() - XPMin, 0, XPMax - XPMin, 0, 100);
        }else XPProgressAsPercentage = 100.0;

        double percentageProgressAsPercentage;

        if(currentPercentage <= percentageNeededMax) {
            percentageProgressAsPercentage = mapValue(currentPercentage - percentageNeededMin, 0, percentageNeededMax - percentageNeededMin,0,100);
        }else if(getTotalXp() == 0){
            percentageProgressAsPercentage = 0;
        } else percentageProgressAsPercentage = 100.0;

        player.sendMessage(skillType.toString() + "XP " + XPProgressAsPercentage + " percentage " + percentageProgressAsPercentage);
        return Math.round(XPProgressAsPercentage * percentageProgressAsPercentage * .01);
    }

    public double getPercentOfTotal(SkillType skillType) {
        return mapValue(getSkill(skillType).getXp(), 0, getTotalXp(), 0, 100);
    }

    private @NotNull Skill getSkill(SkillType skillType) {
        for (Skill skill : this.skills) {
            if (skill.getSkillType() == skillType) {
                return skill;
            }
        }
        throw new IllegalStateException("Can't get skill " + skillType);
    }
}
