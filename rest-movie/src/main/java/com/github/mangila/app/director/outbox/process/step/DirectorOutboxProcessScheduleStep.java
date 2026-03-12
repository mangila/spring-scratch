package com.github.mangila.app.director.outbox.process.step;

import com.github.mangila.app.director.outbox.DirectorOutboxScheduler;
import com.github.mangila.app.director.outbox.destination.DirectorOutboxDestinationOrchestratorJobRequest;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DirectorOutboxProcessScheduleStep {

	private static final Logger log = new JobRunrDashboardLogger(
			LoggerFactory.getLogger(DirectorOutboxProcessScheduleStep.class));

	private final DirectorOutboxScheduler directorOutboxScheduler;

	public DirectorOutboxProcessScheduleStep(DirectorOutboxScheduler directorOutboxScheduler) {
		this.directorOutboxScheduler = directorOutboxScheduler;
	}

	@Retryable
	public UUID execute(UUID outboxId) {
		try {
			var jobId = directorOutboxScheduler.schedule(new DirectorOutboxDestinationOrchestratorJobRequest(outboxId));
			return jobId.asUUID();
		}
		catch (Exception e) {
			log.error("Error scheduling outboxId: {} to orchestrator: {}", outboxId, e.getMessage(), e);
			throw e;
		}
	}

}
