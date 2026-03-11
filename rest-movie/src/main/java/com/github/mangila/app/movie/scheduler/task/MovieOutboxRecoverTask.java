package com.github.mangila.app.movie.scheduler.task;

import com.github.mangila.app.movie.outbox.MovieOutboxScheduler;
import com.github.mangila.app.movie.outbox.recover.MovieOutboxRecoverJobRequest;
import com.github.mangila.app.movie.properties.MovieOutboxRecoverProperties;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

public class MovieOutboxRecoverTask implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(MovieOutboxRecoverTask.class);

    private final MovieOutboxRecoverProperties properties;
    private final LockingTaskExecutor lockingTaskExecutor;
    private final StorageProvider storageProvider;
    private final MovieOutboxScheduler movieOutboxScheduler;

    public MovieOutboxRecoverTask(MovieOutboxRecoverProperties properties,
                                  LockingTaskExecutor lockingTaskExecutor, StorageProvider storageProvider,
                                  MovieOutboxScheduler movieOutboxScheduler) {
        this.properties = properties;
        this.lockingTaskExecutor = lockingTaskExecutor;
        this.storageProvider = storageProvider;
        this.movieOutboxScheduler = movieOutboxScheduler;
    }

    @Override
    public void run() {
        try {
            lockingTaskExecutor.executeWithLock((Runnable) () -> {
                LockAssert.assertLocked();
                final var limit = properties.getLimit();
                final var request = new MovieOutboxRecoverJobRequest(limit);
                long failedJobs = storageProvider.countJobs(StateName.FAILED);
                if (failedJobs == 0) {
                    log.info("No failed jobs found");
                    return;
                }
                var jobId = movieOutboxScheduler.schedule(request);
                log.info("Scheduled recover job with id: {}", jobId);
            }, getLockConfiguration());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private LockConfiguration getLockConfiguration() {
        final var createdAt = Instant.now();
        final var lockName = "movie-recover";
        final var lockAtMostFor = Duration.ofMinutes(3);
        final var lockAtLeastFor = Duration.ofMinutes(3);
        return new LockConfiguration(createdAt,
                lockName,
                lockAtMostFor,
                lockAtLeastFor);
    }
}
