package com.github.mangila.app.director.outbox.relay.step;

import com.github.mangila.app.director.outbox.DirectorOutboxScheduler;
import com.github.mangila.app.director.outbox.process.DirectorOutboxProcessJobRequest;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DirectorOutboxRelayScheduleStep {

	private static final Logger log = new JobRunrDashboardLogger(
			LoggerFactory.getLogger(DirectorOutboxRelayScheduleStep.class));

	private final DirectorOutboxScheduler directorOutboxScheduler;

	public DirectorOutboxRelayScheduleStep(DirectorOutboxScheduler directorOutboxScheduler) {
		this.directorOutboxScheduler = directorOutboxScheduler;
	}

	@Retryable
	public UUID execute(UUID outboxId) {
		try {
			var jobId = directorOutboxScheduler.schedule(new DirectorOutboxProcessJobRequest(outboxId));
			return jobId.asUUID();
		}
		catch (Exception e) {
			log.error("Error while scheduling outbox: {} - {}", outboxId, e.getMessage(), e);
			throw e;
		}
	}

}
