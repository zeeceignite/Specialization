package com.minecraftcivilizations.specialization.Data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.minecraftcivilizations.specialization.Specialization;
import minecraftcivilizations.com.minecraftCivilizationsCore.MinecraftCivilizationsCore;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DataManager {
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public static void startSaver() {
        long initialDelay = getInitialDelayUntilNext10Min();
        long period = 10 * 60; // seconds
        scheduler.scheduleAtFixedRate(saveDataTask(), initialDelay, period, TimeUnit.SECONDS);
    }

    private static Runnable saveDataTask() {
        return () -> {
            Specialization.logger.info("Running task at " + LocalDateTime.now());
            MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().saveAll();
        };

    }

    private static long getInitialDelayUntilNext10Min() {
        LocalDateTime now = LocalDateTime.now();
        int minute = now.getMinute();
        int nextInterval = ((minute / 10) + 1) * 10;

        LocalDateTime nextTime = now.withMinute(0).withSecond(0).withNano(0).plusMinutes(nextInterval);

        Duration duration = Duration.between(now, nextTime);
        return duration.getSeconds() + 1;
    }
}
