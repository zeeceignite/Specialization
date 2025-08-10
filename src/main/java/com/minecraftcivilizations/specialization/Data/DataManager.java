package com.minecraftcivilizations.specialization.Data;

import com.minecraftcivilizations.specialization.Specialization;
import lombok.Getter;
import minecraftcivilizations.com.minecraftCivilizationsCore.MinecraftCivilizationsCore;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DataManager {

    @Getter
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public static void startSaver() {
        long initialDelay = getInitialDelayUntilNext10Min();
        long period = 10 * 60; // seconds
        scheduler.scheduleAtFixedRate(runnable, initialDelay, period, TimeUnit.SECONDS);
    }

    private static final Runnable runnable = () -> {
        Specialization.logger.info("Running task at " + LocalDateTime.now());
        MinecraftCivilizationsCore.getInstance().getCustomPlayerManager().saveAll();
    };

    // this is done so analytics are exactly every 10 min, so its clean
    private static long getInitialDelayUntilNext10Min() {
        LocalDateTime now = LocalDateTime.now();
        int minute = now.getMinute();
        int nextInterval = ((minute / 10) + 1) * 10;

        LocalDateTime nextTime = now.withMinute(0).withSecond(0).withNano(0).plusMinutes(nextInterval);

        Duration duration = Duration.between(now, nextTime);
        return duration.getSeconds() + 1;
    }
}
