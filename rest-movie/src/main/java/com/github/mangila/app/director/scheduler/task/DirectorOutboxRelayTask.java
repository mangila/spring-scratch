package com.github.mangila.app.director.scheduler.task;

import com.github.mangila.app.director.outbox.DirectorOutboxScheduler;
import com.github.mangila.app.director.outbox.relay.DirectorOutboxRelayJobRequest;
import com.github.mangila.app.director.properties.DirectorOutboxRelayProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DirectorOutboxRelayTask implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(DirectorOutboxRelayTask.class);

	private final DirectorOutboxRelayProperties properties;

	private final DirectorOutboxScheduler directorOutboxScheduler;

	public DirectorOutboxRelayTask(DirectorOutboxRelayProperties properties,
			DirectorOutboxScheduler directorOutboxScheduler) {
		this.properties = properties;
		this.directorOutboxScheduler = directorOutboxScheduler;
	}

	@Override
	public void run() {
		final var limit = properties.getLimit();
		final var request = new DirectorOutboxRelayJobRequest(limit);
		// TODO: check PENDING outbox exist b4 schedule
		final var jobId = directorOutboxScheduler.schedule(request);
		log.info("Scheduled relay job with id: {}", jobId);
	}

}
