package com.github.mangila.app.movie.scheduler.task;

import com.github.mangila.app.movie.outbox.MovieOutboxScheduler;
import com.github.mangila.app.movie.outbox.monitor.MovieOutboxMonitorJobRequest;
import com.github.mangila.app.movie.properties.MovieOutboxMonitorProperties;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

public class MovieOutboxMonitorTask implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(MovieOutboxMonitorTask.class);

	private final MovieOutboxMonitorProperties properties;

	private final LockingTaskExecutor lockingTaskExecutor;

	private final MovieOutboxScheduler movieOutboxScheduler;

	public MovieOutboxMonitorTask(MovieOutboxMonitorProperties properties, LockingTaskExecutor lockingTaskExecutor,
			MovieOutboxScheduler movieOutboxScheduler) {
		this.properties = properties;
		this.lockingTaskExecutor = lockingTaskExecutor;
		this.movieOutboxScheduler = movieOutboxScheduler;
	}

	@Override
	public void run() {
		final var limit = properties.getLimit();
		final var request = new MovieOutboxMonitorJobRequest(limit);
		// TODO: check PROCESSING outbox exist b4 schedule
		try {
			lockingTaskExecutor.executeWithLock((Runnable) () -> {
				LockAssert.assertLocked();
				var jobId = movieOutboxScheduler.schedule(request);
				log.info("Scheduled recover job with id: {}", jobId);
			}, getLockConfiguration());
		}
		catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	private LockConfiguration getLockConfiguration() {
		final var createdAt = Instant.now();
		final var lockName = "movie-monitor";
		final var lockAtMostFor = Duration.ofMinutes(3);
		final var lockAtLeastFor = Duration.ofMinutes(3);
		return new LockConfiguration(createdAt, lockName, lockAtMostFor, lockAtLeastFor);
	}

}
