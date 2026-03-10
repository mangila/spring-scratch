package com.github.mangila.app.movie.scheduler.task;

import com.github.mangila.app.movie.outbox.MovieOutboxScheduler;
import com.github.mangila.app.movie.outbox.purge.MovieOutboxPurgeJobRequest;
import com.github.mangila.app.movie.properties.MovieOutboxPurgeProperties;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

public class MovieOutboxPurgeTask implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(MovieOutboxPurgeTask.class);

    private final MovieOutboxPurgeProperties properties;
    private final LockingTaskExecutor lockingTaskExecutor;
    private final MovieOutboxScheduler movieOutboxScheduler;

    public MovieOutboxPurgeTask(MovieOutboxPurgeProperties properties,
                                LockingTaskExecutor lockingTaskExecutor,
                                MovieOutboxScheduler movieOutboxScheduler) {
        this.properties = properties;
        this.lockingTaskExecutor = lockingTaskExecutor;
        this.movieOutboxScheduler = movieOutboxScheduler;
    }

    @Override
    public void run() {
        final var limit = properties.getLimit();
        final var request = new MovieOutboxPurgeJobRequest(limit);
        // TODO: check db exist b4 schedule
        try {
            lockingTaskExecutor.executeWithLock((Runnable) () -> {
                LockAssert.assertLocked();
                var jobId = movieOutboxScheduler.schedule(request);
                log.info("Scheduled purge job with id: {}", jobId);
            }, getLockConfiguration());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private LockConfiguration getLockConfiguration() {
        final var createdAt = Instant.now();
        final var lockName = "purge";
        final var lockAtMostFor = Duration.ofMinutes(3);
        final var lockAtLeastFor = Duration.ofMinutes(3);
        return new LockConfiguration(createdAt,
                lockName,
                lockAtMostFor,
                lockAtLeastFor);
    }
}
