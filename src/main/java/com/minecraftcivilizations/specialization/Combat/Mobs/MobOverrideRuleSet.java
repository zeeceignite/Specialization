package com.minecraftcivilizations.specialization.Combat.Mobs;

import com.minecraftcivilizations.specialization.StaffTools.Debug;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class MobOverrideRuleSet {

    @Setter @Getter
    private int baseChance = 100; // this is the chance that the Vanilla entity will spawn for this EntityType

    private final Set<MobOverrideRule> rules = new HashSet<>();
    private int total_chance = 0;

    public void add(MobOverrideRule rule) {
        if (rule == null) return;
        boolean added = rules.add(rule);
//        Debug.broadcast("mobrule", "<dark_green>adding rule chance for </dark_green>=" + rule.getChance() + " " + Debug.formatBoolean(added));
        if (added) total_chance += Math.max(0, rule.getChance());
    }

    private int sumOverrideChances() {
        int sum = 0;
        for (MobOverrideRule rule : rules) sum += Math.max(0, rule.getChance());
        return sum;
    }

    public MobOverrideRule rollRule() {
        int overrides_total = sumOverrideChances();
        int total = baseChance + overrides_total;
//        Debug.broadcast("mobrule", "base:" + baseChance + " overrides:" + overrides_total + " total:" + total);
        if (total <= 0) return null;
        int r = ThreadLocalRandom.current().nextInt(total);
        if (r < baseChance) return null;
        r -= baseChance;
        int accum = 0;
        for (MobOverrideRule rule : rules) {
            accum += Math.max(0, rule.getChance());
            if (r < accum){
//                Debug.broadcast("mobrule", "returning a valid RULE!");
                return rule;
            }
        }
        return null;
    }

    public int getTotalChance() {
        return baseChance + total_chance;
    }
}
