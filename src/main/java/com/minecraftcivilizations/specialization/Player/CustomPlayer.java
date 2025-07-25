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
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static com.minecraftcivilizations.specialization.Skill.Skill.getXPNeededForLevel;
import static com.minecraftcivilizations.specialization.Skill.Skill.mapValue;

public class CustomPlayer extends minecraftcivilizations.com.minecraftCivilizationsCore.Player.CustomPlayer {
    @Getter
    @Setter
    private SkillType preferredSkill = SkillType.values()[ThreadLocalRandom.current().nextInt(SkillType.values().length)];
    @Getter
    List<Skill> skills = new ArrayList<>(0);
    @Getter
    @Setter
    private String inGameName = "Ginger";
    @Setter
    @Getter
    private double height = 0;

    public CustomPlayer(UUID uuid) {
        super(uuid);
        loadPlayer();

    }

    private void loadPlayer() {
        CustomPlayer customPlayer = (CustomPlayer) MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().load(this.getUuid(), CustomPlayer.class);

        if (customPlayer != null) {
            return;
        }

        for (SkillType skill : SkillType.values()) {
            Skill skill1 = new Skill(skill, SkillLevel.NOVICE, 0, System.currentTimeMillis());
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

        player.getAttribute(Attribute.MINING_EFFICIENCY).setBaseValue(0);
        player.getAttribute(Attribute.BLOCK_BREAK_SPEED).setBaseValue(0);
    }

    public void addSkillXp(SkillType skillType, double xp) {
        if (skillType == null) return;
        Bukkit.getPlayer(getUuid()).sendMessage(Component.text("You gained xp in: " + skillType.name()));
        int previousLevel = this.getSkillLevel(skillType);
        getSkill(skillType).addXp(xp);
        int currentLevel = this.getSkillLevel(skillType);
        if (previousLevel != currentLevel) {
            while (currentLevel > 0) {
                Set<Pair> recipes = new Gson().fromJson(
                        Config.getUnlockedRecipesConfig().getString(skillType.name() + "_" + SkillLevel.getSkillLevelFromInt(currentLevel)),
                        new TypeToken<Set<Pair>>() {}.getType());
                for (Pair entry : recipes) {
                    NamespacedKey key = new NamespacedKey(entry.key(), entry.value());
                    Specialization.logger.info(String.valueOf(Bukkit.getPlayer(this.getUuid()).discoverRecipe(key)));
                }

                currentLevel--;
            }
        }
    }

    public int getSkillLevel(SkillType skillType) {

        int level = 0;
        Specialization.logger.info(String.valueOf(this.getPercentOfTotal(skillType)));
        Specialization.logger.info(String.valueOf(Config.getSkillRequirementsConfig().getDouble(skillType.name() + "_" + SkillLevel.getSkillLevelFromInt(1) + "_REQUIREMENT")));
        Specialization.logger.info(String.valueOf(skillType.name() + "_" + SkillLevel.getSkillLevelFromInt(1) + "_REQUIREMENT"));
        Specialization.logger.info(String.valueOf(((double) this.getPercentOfTotal(skillType)) < (double) Config.getSkillRequirementsConfig().getDouble(skillType.name() + "_" + SkillLevel.getSkillLevelFromInt(level) + "_REQUIREMENT")));
        while (getSkill(skillType).getXp() > getXPNeededForLevel(level) && this.getPercentOfTotal(skillType) >= Config.getSkillRequirementsConfig().getDouble(skillType.name() + "_" + SkillLevel.getSkillLevelFromInt(level) + "_REQUIREMENT")) {
            if (getSkill(skillType).getXp() < getXPNeededForLevel(level+1)) {
                return level;
            }
            level++;
        }
        return level;
    }

    public double getTotalXp() {
        double totalXp = 0;
        for (Skill skill : this.skills) {
            totalXp += skill.getXp();
        }
        return totalXp;
    }

    public double getGUIDistributionOfTotalSkills(SkillType skillType) {
        return mapValue(getSkill(skillType).getXp(), 0, getTotalXp(), 0, 3);
    }

    public double getPercentOfTotal(SkillType skillType) {
        return mapValue(getSkill(skillType).getXp(), 0, getTotalXp(), 0, 100);
    }

    private Skill getSkill(SkillType skillType) {
        for (Skill skill : this.skills) {
            if (skill.getSkillType() == skillType) {
                return skill;
            }
        }
        return null;
    }
}
