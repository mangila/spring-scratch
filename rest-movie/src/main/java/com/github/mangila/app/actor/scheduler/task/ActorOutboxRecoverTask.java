package com.github.mangila.app.actor.scheduler.task;

import com.github.mangila.app.actor.outbox.ActorOutboxScheduler;
import com.github.mangila.app.actor.outbox.recover.ActorOutboxRecoverJobRequest;
import com.github.mangila.app.actor.properties.ActorOutboxRecoverProperties;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

public class ActorOutboxRecoverTask implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(ActorOutboxRecoverTask.class);

	private final ActorOutboxRecoverProperties properties;

	private final LockingTaskExecutor lockingTaskExecutor;

	private final StorageProvider storageProvider;

	private final ActorOutboxScheduler actorOutboxScheduler;

	public ActorOutboxRecoverTask(ActorOutboxRecoverProperties properties, LockingTaskExecutor lockingTaskExecutor,
			StorageProvider storageProvider, ActorOutboxScheduler actorOutboxScheduler) {
		this.properties = properties;
		this.lockingTaskExecutor = lockingTaskExecutor;
		this.storageProvider = storageProvider;
		this.actorOutboxScheduler = actorOutboxScheduler;
	}

	@Override
	public void run() {
		try {
			lockingTaskExecutor.executeWithLock((Runnable) () -> {
				LockAssert.assertLocked();
				final var limit = properties.getLimit();
				final var request = new ActorOutboxRecoverJobRequest(limit);
				long failedJobs = storageProvider.countJobs(StateName.FAILED);
				if (failedJobs == 0) {
					log.info("No failed jobs found");
					return;
				}
				var jobId = actorOutboxScheduler.schedule(request);
				log.info("Scheduled recover job with id: {}", jobId);
			}, getLockConfiguration());
		}
		catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	private LockConfiguration getLockConfiguration() {
		final var createdAt = Instant.now();
		final var lockName = "actor-recover";
		final var lockAtMostFor = Duration.ofMinutes(3);
		final var lockAtLeastFor = Duration.ofMinutes(3);
		return new LockConfiguration(createdAt, lockName, lockAtMostFor, lockAtLeastFor);
	}

}
