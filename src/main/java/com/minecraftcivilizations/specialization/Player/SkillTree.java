package com.minecraftcivilizations.specialization.Player;

import com.minecraftcivilizations.specialization.Skill.SkillType;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SkillTree {
    @Getter
    Map<SkillType, SkillBranch> branches = new HashMap<>(0);


    public SkillTree() {
        for (SkillType skillType : SkillType.values()) {
            this.branches.put(skillType, new SkillBranch());
        }
    }
}
