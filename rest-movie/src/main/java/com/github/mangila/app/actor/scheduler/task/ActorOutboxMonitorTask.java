package com.github.mangila.app.actor.scheduler.task;

import com.github.mangila.app.actor.outbox.ActorOutboxScheduler;
import com.github.mangila.app.actor.outbox.monitor.ActorOutboxMonitorJobRequest;
import com.github.mangila.app.actor.properties.ActorOutboxMonitorProperties;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

public class ActorOutboxMonitorTask implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ActorOutboxMonitorTask.class);

    private final ActorOutboxMonitorProperties properties;
    private final LockingTaskExecutor lockingTaskExecutor;
    private final ActorOutboxScheduler actorOutboxScheduler;

    public ActorOutboxMonitorTask(ActorOutboxMonitorProperties properties,
                                  LockingTaskExecutor lockingTaskExecutor,
                                  ActorOutboxScheduler actorOutboxScheduler) {
        this.properties = properties;
        this.lockingTaskExecutor = lockingTaskExecutor;
        this.actorOutboxScheduler = actorOutboxScheduler;
    }

    @Override
    public void run() {
        final var limit = properties.getLimit();
        final var request = new ActorOutboxMonitorJobRequest(limit);
        // TODO: check PROCESSING outbox exist b4 schedule
        try {
            lockingTaskExecutor.executeWithLock((Runnable) () -> {
                LockAssert.assertLocked();
                var jobId = actorOutboxScheduler.schedule(request);
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
