package com.github.mangila.app.director.scheduler.task;

import com.github.mangila.app.director.outbox.DirectorOutboxScheduler;
import com.github.mangila.app.director.outbox.recover.DirectorOutboxRecoverJobRequest;
import com.github.mangila.app.director.properties.DirectorOutboxRecoverProperties;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

public class DirectorOutboxRecoverTask implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(DirectorOutboxRecoverTask.class);

	private final DirectorOutboxRecoverProperties properties;

	private final LockingTaskExecutor lockingTaskExecutor;

	private final StorageProvider storageProvider;

	private final DirectorOutboxScheduler directorOutboxScheduler;

	public DirectorOutboxRecoverTask(DirectorOutboxRecoverProperties properties,
			LockingTaskExecutor lockingTaskExecutor, StorageProvider storageProvider,
			DirectorOutboxScheduler directorOutboxScheduler) {
		this.properties = properties;
		this.lockingTaskExecutor = lockingTaskExecutor;
		this.storageProvider = storageProvider;
		this.directorOutboxScheduler = directorOutboxScheduler;
	}

	@Override
	public void run() {
		try {
			lockingTaskExecutor.executeWithLock((Runnable) () -> {
				LockAssert.assertLocked();
				final var limit = properties.getLimit();
				final var request = new DirectorOutboxRecoverJobRequest(limit);
				long failedJobs = storageProvider.countJobs(StateName.FAILED);
				if (failedJobs == 0) {
					log.info("No failed jobs found");
					return;
				}
				var jobId = directorOutboxScheduler.schedule(request);
				log.info("Scheduled recover job with id: {}", jobId);
			}, getLockConfiguration());
		}
		catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	private LockConfiguration getLockConfiguration() {
		final var createdAt = Instant.now();
		final var lockName = "director-recover";
		final var lockAtMostFor = Duration.ofMinutes(3);
		final var lockAtLeastFor = Duration.ofMinutes(3);
		return new LockConfiguration(createdAt, lockName, lockAtMostFor, lockAtLeastFor);
	}

}
