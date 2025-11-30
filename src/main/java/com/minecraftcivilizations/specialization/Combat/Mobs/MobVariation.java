package com.minecraftcivilizations.specialization.Combat.Mobs;


import com.minecraftcivilizations.specialization.util.MathUtils;
import lombok.Getter;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffect;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Allows for custom mob variations.
 * Automatically registered on creation.
 * Does NOT determine chance to spawn or anything, just the rulset.
 * @author alectriciti
 * @see MobManager
 * @see MobOverrideRule
 */
public class MobVariation {


    @Getter
    private String id;
    /**
     * this mob variation will spawn using a randomized version of this
     */
    // fields
    EnumSet<EntityType> types;
    private transient EntityType[] types_array;
    boolean do_not_change_type = false;

    @Getter
    PotionEffect potionEffect;

    /**
     * Must be called
     * @param types is an overload to support variations such as Zombie, Husk, Drowned. Intended for similar types
     */
    public MobVariation(String id, EntityType... types) {
        this.id = id;
        if (types == null) {
            do_not_change_type = true;
        } else {
            this.types = EnumSet.noneOf(EntityType.class);
            Collections.addAll(this.types, types);
            // cache a compact array representation for fast random access
            this.types_array = this.types.toArray(new EntityType[0]);
        }
        if(types_array == null || types_array.length == 0){
            do_not_change_type = true;
        }
        MobManager.getInstance().registerMobVariation(this);
    }

    public EntityType rollType() {
        // fast paths: no change requested or nothing to choose from
        if (do_not_change_type) return null;

        // single-element fast path
        if (types_array.length == 1) return types_array[0];

        return types_array[ThreadLocalRandom.current().nextInt(types_array.length)];
    }

//    public MobVariation(String id, double dmg_multiplier, double health_multiplier) {
//        this(id);
//        this.damageMultiplier = dmg_multiplier;
//        this.healthMultiplier = health_multiplier;
//    }

    //defaults for all hostile mobs
    @Getter
    private double damageMultiplierDay = 1.0;
    @Getter
    private double damageMultiplierNight = 1.0;
    @Getter
    private double damageBaseDay = 0.0;
    @Getter
    private double damageBaseNight = 0.0;
    @Getter
    private double healthMultiplier = 1.0;
    @Getter
    private double speedMultiplierDay = 1.0;
    @Getter
    private double speedMultiplierNight = 1.25;
    @Getter
    private double waterSpeedMultiplierDay = 1.0;
    @Getter
    private double waterSpeedMultiplierNight = 1.0;
    @Getter
    private double sizeSmallest = 1.0;
    @Getter
    private double sizeLargest = 1.0;
    @Getter
    private double fuseTime = 1.0;
    @Getter
    private double stepHeight = 0.0;

    private boolean random_scale_enabled = false;

    private boolean always_angry = false;

    private boolean does_hunting = false;
    private boolean does_breaking = false;
    private boolean invisible = false;

    private boolean allow_xp = false;

    private boolean replace_original_mob = false;

    @Getter
    public double followRange = 42;


    private double xp_multiplier = 1.0;

    Set<DamageType> immunity_types = new HashSet<>();

    @Getter
    int spawnExtra = 0;

    @Getter
    double dropChance = -1.0;

    @Getter
    double dropAmountScale = 1.0;

    private MobVariation mount;
    @Getter
    private double mountChance = 1.0;

    @Getter
    private double breakScalar = 1.0;

    public double getXpScale() {
        return xp_multiplier;
    }

    public MobVariation xpScale(double xp_multiplier){
        this.xp_multiplier = xp_multiplier;
        return this;
    }

    public MobVariation damage(double multiplier){
        this.damageMultiplierDay = multiplier;
        return this;
    }

    public MobVariation damage(double multiplier_day, double multiplier_night){
        this.damageMultiplierDay = multiplier_day;
        this.damageMultiplierNight = multiplier_night;
        return this;
    }

    public MobVariation health(double multiplier){
        this.healthMultiplier = multiplier;
        return this;
    }

    public MobVariation speed(double multiplier){
        this.speedMultiplierNight = multiplier;
        this.speedMultiplierDay = multiplier;
        return this;
    }

    public MobVariation speed(double day_multiplier, double night_multiplier){
        this.speedMultiplierNight = night_multiplier;
        this.speedMultiplierDay = day_multiplier;
        return this;
    }

    public MobVariation stepheight(double step_height){
        this.stepHeight = step_height;
        return this;
    }


    public MobVariation fuseTime(double fuse_time){
        this.fuseTime = fuse_time;
        return this;
    }


    public MobVariation waterspeed(double day_multiplier, double night_multiplier){
        this.waterSpeedMultiplierNight = night_multiplier;
        this.waterSpeedMultiplierDay = day_multiplier;
        return this;
    }

    public MobVariation size(double smallest, double largest) {
        this.random_scale_enabled = true;
        this.sizeSmallest = smallest;
        this.sizeLargest = largest;
        return this;
    }

    public MobVariation size(double size) {
        this.sizeSmallest = size;
        return this;
    }

    public double getScale() {
        if(random_scale_enabled) {
            return MathUtils.random(sizeSmallest, sizeLargest);
        }
        return sizeSmallest;
    }

    /**
     * Only applies to custom overrides
     */
    public MobVariation anger(boolean anger) {
        always_angry = anger;
        return this;
    }

    public boolean isAngry() {
        return always_angry;
    }

    /**
     * Hunts using default
     */
    public MobVariation hunts() {
        this.does_hunting = true;
        return this;
    }

    public MobVariation hunts(double follow_range) {
        this.does_hunting = true;
        this.followRange = follow_range;
        return this;
    }

    public boolean doesHunting() {
        return does_hunting;
    }

    public MobVariation breaks(){
        this.does_breaking = true;
        return this;
    }

    public MobVariation breaks(double scalar){
        this.does_breaking = true;
        this.breakScalar = scalar;
        return this;
    }

    public boolean doesBreaking() {
        return does_breaking;
    }

    public MobVariation invisible() {
        invisible = true;
        return this;
    }

    public boolean isInvisible(){
        return invisible;
    }

    public MobVariation allowXpGainForNonEnemy(){
        this.allow_xp = true;
        return this;
    }

    public boolean doesAllowXpGainForNonEnemy(){
        return allow_xp;
    }


    public MobVariation replaceOriginalMob(){
        this.replace_original_mob = true;
        return this;
    }

    public boolean doesReplaceOriginalMob(){
        return replace_original_mob;
    }

    public MobVariation setMount(MobVariation mount) {
        this.mount = mount;
        return this;
    }

    public MobVariation setMount(MobVariation mount, double chance) {
        this.mount = mount;
        this.mountChance = chance;
        return this;
    }

    public MobVariation getMount() {
        return this.mount;
    }

    public MobVariation setPotionEffect(PotionEffect potionEffect) {
        this.potionEffect = potionEffect;
        return this;
    }

    @Getter
    private boolean xpGainOverrideActive = false;
    private boolean xpGainOverrideState;
    public boolean getXpGainOverrideState(){
        return xpGainOverrideState;
    }


    public MobVariation setGainsXpOverride(boolean b) {
        this.xpGainOverrideState = b;
        this.xpGainOverrideActive = true;
        return this;
    }

    public MobVariation spawnExtra(int extra_amount) {
        this.spawnExtra = extra_amount;
        return this;
    }

    public MobVariation drops(double chance){
        this.dropChance = chance;
        return this;
    }

    public MobVariation drops(double chance, double amount_scalar){
        this.dropChance = chance;
        this.dropAmountScale = amount_scalar;
        return this;
    }

    public MobVariation addImmunity(DamageType type) {
        immunity_types.add(type);
        return this;
    }


}