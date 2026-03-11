package com.github.mangila.app.director.scheduler.task;

import com.github.mangila.app.director.outbox.DirectorOutboxScheduler;
import com.github.mangila.app.director.outbox.monitor.DirectorOutboxMonitorJobRequest;
import com.github.mangila.app.director.properties.DirectorOutboxMonitorProperties;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

public class DirectorOutboxMonitorTask implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(DirectorOutboxMonitorTask.class);

    private final DirectorOutboxMonitorProperties properties;
    private final LockingTaskExecutor lockingTaskExecutor;
    private final DirectorOutboxScheduler directorOutboxScheduler;

    public DirectorOutboxMonitorTask(DirectorOutboxMonitorProperties properties, LockingTaskExecutor lockingTaskExecutor,
                                  DirectorOutboxScheduler directorOutboxScheduler) {
        this.properties = properties;
        this.lockingTaskExecutor = lockingTaskExecutor;
        this.directorOutboxScheduler = directorOutboxScheduler;
    }

    @Override
    public void run() {
        final var limit = properties.getLimit();
        final var request = new DirectorOutboxMonitorJobRequest(limit);
        // TODO: check PROCESSING outbox exist b4 schedule
        try {
            lockingTaskExecutor.executeWithLock((Runnable) () -> {
                LockAssert.assertLocked();
                var jobId = directorOutboxScheduler.schedule(request);
                log.info("Scheduled monitor job with id: {}", jobId);
            }, getLockConfiguration());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private LockConfiguration getLockConfiguration() {
        final var createdAt = Instant.now();
        final var lockName = "director-monitor";
        final var lockAtMostFor = Duration.ofMinutes(3);
        final var lockAtLeastFor = Duration.ofMinutes(3);
        return new LockConfiguration(createdAt,
                lockName,
                lockAtMostFor,
                lockAtLeastFor);
    }
}
