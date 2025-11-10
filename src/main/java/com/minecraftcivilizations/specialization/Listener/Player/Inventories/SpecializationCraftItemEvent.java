package com.minecraftcivilizations.specialization.Listener.Player.Inventories;

import com.minecraftcivilizations.specialization.Skill.SkillType;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.CraftItemEvent;

/**
 * Was going to use this for something, maybe not, yneverknow
 * - Alec
 */
public class SpecializationCraftItemEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    @Getter
    private CraftItemEvent event;
    @Getter
    private Player player;
    @Getter
    private int craftedAmount;
    @Getter
    private int totalReduction;
    @Getter
    private SkillType skillType;
    @Getter
    private int skillLevel;

    @Setter
    private boolean grantXp = true;

    public SpecializationCraftItemEvent(CraftItemEvent event, Player player, int craftedAmount, int totalReduction, SkillType skillType, int skillLevel) {
        this.event = event;
        this.player = player;
        this.craftedAmount = craftedAmount;
        this.totalReduction = totalReduction;
        this.skillType = skillType;
        this.skillLevel = skillLevel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }


    public boolean doesGrantXp() {
        return grantXp;
    }
}
