package com.minecraftcivilizations.specialization.Player;

import com.minecraftcivilizations.specialization.Player.Perk.Perk;
import com.minecraftcivilizations.specialization.Skill.SkillLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SkillBranch {
    @Getter
    Map<SkillLevel, List<Perk>> perks = new HashMap<>(0);
    @Getter
    @Setter
    int availablePoints = 1;

    public void removePoint() {
        availablePoints--;
    }

    public void removePoints(int amount) {
        availablePoints -= amount;
    }

    public void addPoint() {
        availablePoints++;
    }

    public void addPoints(int amount) {
        availablePoints += amount;
    }

    public SkillBranch() {
        for (SkillLevel skillLevel : SkillLevel.values()) {
            perks.put(skillLevel, new ArrayList<>() {
                {
                    add(new Perk());
                    add(new Perk());
                    add(new Perk());
                    add(new Perk());
                }
            });
        }
    }
}
