package com.minecraftcivilizations.specialization.Skill;

import com.minecraftcivilizations.specialization.Config.Config;
import org.bukkit.Material;

public enum SkillType {
    FARMER,
    BUILDER,
    MINER,
    HEALER,
    LIBRARIAN,
    GUARDSMAN,
    BLACKSMITH;



    public String getSkillDescription() {
        return Config.getSkillsConfig().getString(this.name() + "_DESCRIPTION");
    }

    public Material getSkillWorkstation() {
        return Material.valueOf(Config.getSkillsConfig().getString(this.name() + "_WORKSTATION"));
    }
}
