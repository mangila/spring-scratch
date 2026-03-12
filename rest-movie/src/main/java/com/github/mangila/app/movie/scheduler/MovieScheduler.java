package com.github.mangila.app.movie.scheduler;

import com.github.mangila.app.movie.outbox.MovieOutboxScheduler;
import com.github.mangila.app.movie.properties.MovieProperties;
import com.github.mangila.app.movie.scheduler.task.MovieOutboxMonitorTask;
import com.github.mangila.app.movie.scheduler.task.MovieOutboxPurgeTask;
import com.github.mangila.app.movie.scheduler.task.MovieOutboxRecoverTask;
import com.github.mangila.app.movie.scheduler.task.MovieOutboxRelayTask;
import jakarta.annotation.PostConstruct;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.jobrunr.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

@Service
public class MovieScheduler {

	private static final Logger log = LoggerFactory.getLogger(MovieScheduler.class);

	private final SimpleAsyncTaskScheduler taskScheduler;

	private final StorageProvider storageProvider;

	private final MovieOutboxScheduler movieOutboxScheduler;

	private final LockingTaskExecutor lockingTaskExecutor;

	private final MovieProperties movieProperties;

	public MovieScheduler(SimpleAsyncTaskScheduler taskScheduler, StorageProvider storageProvider,
			MovieProperties movieProperties, MovieOutboxScheduler movieOutboxScheduler,
			LockingTaskExecutor lockingTaskExecutor) {
		this.taskScheduler = taskScheduler;
		this.storageProvider = storageProvider;
		this.movieProperties = movieProperties;
		this.movieOutboxScheduler = movieOutboxScheduler;
		this.lockingTaskExecutor = lockingTaskExecutor;
	}

	@PostConstruct
	public void init() {
		var outbox = movieProperties.getOutbox();
		if (outbox.isEnabled()) {
			log.info("Movie Outbox is enabled");
			if (outbox.getRelay().isEnabled()) {
				log.info("Movie Outbox relay is enabled");
				final var props = movieProperties.getOutbox().getRelay();
				final var task = new MovieOutboxRelayTask(props, movieOutboxScheduler);
				taskScheduler.schedule(task, new CronTrigger(props.getCron()));
			}
			if (outbox.getMonitor().isEnabled()) {
				log.info("Movie Outbox monitor is enabled");
				final var props = movieProperties.getOutbox().getMonitor();
				final var task = new MovieOutboxMonitorTask(props, lockingTaskExecutor, movieOutboxScheduler);
				taskScheduler.schedule(task, new CronTrigger(props.getCron()));
			}
			if (outbox.getRecover().isEnabled()) {
				log.info("Movie Outbox recover is enabled");
				final var props = movieProperties.getOutbox().getRecover();
				final var task = new MovieOutboxRecoverTask(props, lockingTaskExecutor, storageProvider,
						movieOutboxScheduler);
				taskScheduler.schedule(task, new CronTrigger(props.getCron()));
			}
			if (outbox.getPurge().isEnabled()) {
				log.info("Movie Outbox purge is enabled");
				final var props = movieProperties.getOutbox().getPurge();
				final var task = new MovieOutboxPurgeTask(props, movieOutboxScheduler);
				taskScheduler.schedule(task, new CronTrigger(props.getCron()));
			}
		}
	}

}
