package com.minecraftcivilizations.specialization.Distance;

import lombok.Getter;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class Town {

    private Location centerLocation;
    private List<Location> bedLocations;
    @Getter
    private final long discoveredTime;

    public Town(Location center, List<Location> beds) {
        this.centerLocation = center;
        this.bedLocations = new ArrayList<>(beds);
        this.discoveredTime = System.currentTimeMillis();
    }

    public Location getCenterLocation() {
        return centerLocation.clone();
    }

    public List<Location> getBedLocations() {
        return new ArrayList<>(bedLocations);
    }

    public int getBedCount() {
        return bedLocations.size();
    }

    public void updateBeds(List<Location> newBeds) {
        this.bedLocations = new ArrayList<>(newBeds);
        // Recalculate center
        if (!newBeds.isEmpty()) {


            this.centerLocation = TownManager.calculateTownCenter(newBeds);
        }
    }

    @Override
    public String toString() {
        return String.format("Town{center=%s, beds=%d, discovered=%d}",
                centerLocation, bedLocations.size(), discoveredTime);
    }

}
