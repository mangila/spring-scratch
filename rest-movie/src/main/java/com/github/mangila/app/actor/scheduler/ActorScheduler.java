package com.github.mangila.app.actor.scheduler;

import com.github.mangila.app.actor.outbox.ActorOutboxScheduler;
import com.github.mangila.app.actor.properties.ActorProperties;
import com.github.mangila.app.actor.scheduler.task.ActorOutboxMonitorTask;
import com.github.mangila.app.actor.scheduler.task.ActorOutboxPurgeTask;
import com.github.mangila.app.actor.scheduler.task.ActorOutboxRecoverTask;
import com.github.mangila.app.actor.scheduler.task.ActorOutboxRelayTask;
import jakarta.annotation.PostConstruct;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.jobrunr.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

@Service
public class ActorScheduler {

	private static final Logger log = LoggerFactory.getLogger(ActorScheduler.class);

	private final SimpleAsyncTaskScheduler taskScheduler;

	private final StorageProvider storageProvider;

	private final ActorOutboxScheduler actorOutboxScheduler;

	private final LockingTaskExecutor lockingTaskExecutor;

	private final ActorProperties actorProperties;

	public ActorScheduler(SimpleAsyncTaskScheduler taskScheduler, StorageProvider storageProvider,
			ActorOutboxScheduler actorOutboxScheduler, LockingTaskExecutor lockingTaskExecutor,
			ActorProperties actorProperties) {
		this.taskScheduler = taskScheduler;
		this.storageProvider = storageProvider;
		this.actorOutboxScheduler = actorOutboxScheduler;
		this.lockingTaskExecutor = lockingTaskExecutor;
		this.actorProperties = actorProperties;
	}

	@PostConstruct
	public void init() {
		var outbox = actorProperties.getOutbox();
		if (outbox.isEnabled()) {
			log.info("Director Outbox is enabled");
			if (outbox.getRelay().isEnabled()) {
				log.info("Director Outbox relay is enabled");
				final var props = actorProperties.getOutbox().getRelay();
				final var task = new ActorOutboxRelayTask(props, actorOutboxScheduler);
				taskScheduler.schedule(task, new CronTrigger(props.getCron()));
			}
			if (outbox.getMonitor().isEnabled()) {
				log.info("Director Outbox monitor is enabled");
				final var props = actorProperties.getOutbox().getMonitor();
				final var task = new ActorOutboxMonitorTask(props, lockingTaskExecutor, actorOutboxScheduler);
				taskScheduler.schedule(task, new CronTrigger(props.getCron()));
			}
			if (outbox.getRecover().isEnabled()) {
				log.info("Director Outbox recover is enabled");
				final var props = actorProperties.getOutbox().getRecover();
				final var task = new ActorOutboxRecoverTask(props, lockingTaskExecutor, storageProvider,
						actorOutboxScheduler);
				taskScheduler.schedule(task, new CronTrigger(props.getCron()));
			}
			if (outbox.getPurge().isEnabled()) {
				log.info("Director Outbox purge is enabled");
				final var props = actorProperties.getOutbox().getPurge();
				final var task = new ActorOutboxPurgeTask(props, actorOutboxScheduler);
				taskScheduler.schedule(task, new CronTrigger(props.getCron()));
			}
		}
	}

}
