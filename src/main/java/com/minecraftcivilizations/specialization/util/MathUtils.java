package com.minecraftcivilizations.specialization.util;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

public class MathUtils {


    public static float random(float min, float max){
        return ThreadLocalRandom.current().nextFloat(max-min)+min;
    }
    public static double random(double min, double max){
        return ThreadLocalRandom.current().nextDouble(max-min)+min;
    }
    public static double random(int min, int max){
        return ThreadLocalRandom.current().nextInt(max-min)+min;
    }

    /**
     * Rolls 0.0 to 1.0
     */
    public static double rollDouble(){
        return ThreadLocalRandom.current().nextDouble();
    }
    public static boolean rollBoolean(){
        return ThreadLocalRandom.current().nextBoolean();
    }

    public static double quantize(double value, double amount) {
        return Math.round(value / amount) * amount;
    }

    public static Vector randomDirection(){
        return new Vector(
                random(-1f,1f),
                random(-1f,1f),
                random(-1f,1f));
    }

    /**
     * Linear Interpolation from a to b
     */
    public static double lerp(double a, double b, double alpha) {
        return a + (b - a) * alpha;
    }


    /**
     * Maps a value from one range to another.
     * Works like UE5's GetMappedRangeValue.
     * Extrapolates outside the input range.
     */
    public static float range(float inMin, float inMax, float outMin, float outMax, float value) {
        float t = (value - inMin) / (inMax - inMin); // normalize into 0–1 (can go below 0 or above 1)
        return outMin + t * (outMax - outMin);
    }
    public static double range(double inMin, double inMax, double outMin, double outMax, double value) {
        double t = (value - inMin) / (inMax - inMin); // normalize into 0–1 (can go below 0 or above 1)
        return outMin + t * (outMax - outMin);
    }
    /**
     * Maps a value from one range to another.
     * Clamps to the output range instead of extrapolating.
     */
    public static float rangeClamped(float inMin, float inMax, float outMin, float outMax, float value) {
        float t = (value - inMin) / (inMax - inMin);
        t = Math.max(0f, Math.min(1f, t)); // clamp t between 0 and 1
        return outMin + t * (outMax - outMin);
    }

    public static double rangeClamped(double inMin, double inMax, double outMin, double outMax, double value) {
        double t = (value - inMin) / (inMax - inMin);
        t = Math.max(0.0, Math.min(1.0, t)); // clamp t between 0 and 1
        return outMin + t * (outMax - outMin);
    }

    /**
     * Disregards Yaw and Pitch
     */
    public static Location lerpLocationFast(Location from, Location to, double t) {

        double x = from.getX() + (to.getX() - from.getX()) * t;
        double y = from.getY() + (to.getY() - from.getY()) * t;
        double z = from.getZ() + (to.getZ() - from.getZ()) * t;

        return new Location(from.getWorld(), x, y, z, from.getYaw(), from.getPitch());
    }

    /**
     * Complete Location lerping
     * if you only need x,y,z, use lerpLocationFast
     */
    public static Location lerpLocation(Location from, Location to, double t) {

        double x = from.getX() + (to.getX() - from.getX()) * t;
        double y = from.getY() + (to.getY() - from.getY()) * t;
        double z = from.getZ() + (to.getZ() - from.getZ()) * t;

        float yaw = from.getYaw() + (to.getYaw() - from.getYaw()) * (float) t;
        float pitch = from.getPitch() + (to.getPitch() - from.getPitch()) * (float) t;

        return new Location(from.getWorld(), x, y, z, yaw, pitch);
    }

    public static Vector randomVectorCentered(float amount) {
        double x = (ThreadLocalRandom.current().nextDouble() - 0.5) * 2 * amount;
        double y = (ThreadLocalRandom.current().nextDouble() - 0.5) * 2 * amount;
        double z = (ThreadLocalRandom.current().nextDouble() - 0.5) * 2 * amount;
        return new Vector(x, y, z);
    }


    public static Vector randomVectorCentered(double amount) {
        double x = (ThreadLocalRandom.current().nextDouble() - 0.5) * 2 * amount;
        double y = (ThreadLocalRandom.current().nextDouble() - 0.5) * 2 * amount;
        double z = (ThreadLocalRandom.current().nextDouble() - 0.5) * 2 * amount;
        return new Vector(x, y, z);
    }

    /**
     * Converts a player's yaw and pitch to a direction vector
     * @param yaw turn angle
     * @param pitch look up/down angle
     */
    public static Vector getDirectionVector(float yaw, float pitch) {
        double yawRad = Math.toRadians(-yaw);
        double pitchRad = Math.toRadians(-pitch);

        double x = Math.cos(pitchRad) * Math.sin(yawRad);
        double y = Math.sin(pitchRad);
        double z = Math.cos(pitchRad) * Math.cos(yawRad);

        return new Vector(x, y, z);
    }



}
