package io.chrislowe.discordle.util;

import java.time.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FixedTimeScheduler {
    private final ScheduledExecutorService executor;
    private final Runnable task;

    public FixedTimeScheduler(Runnable task) {
        this.executor = Executors.newSingleThreadScheduledExecutor();
        this.task = task;
    }

    public void addDailyExecution(LocalTime startTime) {
        executor.schedule(() -> {
            try {
                task.run();
            } finally {
                addDailyExecution(startTime);
            }
        }, calculateDelayUntil(startTime), TimeUnit.SECONDS);
    }

    private long calculateDelayUntil(LocalTime startTime) {
        LocalDateTime localNow = LocalDateTime.now();
        LocalDateTime startDateTime = LocalDateTime.of(LocalDate.now(), startTime);
        if (localNow.isAfter(startDateTime)) {
            startDateTime = startDateTime.plusDays(1);
        }
        return Duration.between(localNow, startDateTime).getSeconds() + 1;
    }
}
