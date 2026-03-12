package com.github.mangila.app.movie.outbox.relay.step;

import com.github.mangila.app.movie.outbox.MovieOutboxScheduler;
import com.github.mangila.app.movie.outbox.process.MovieOutboxProcessJobRequest;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MovieOutboxRelayScheduleStep {

	private static final Logger log = new JobRunrDashboardLogger(
			LoggerFactory.getLogger(MovieOutboxRelayScheduleStep.class));

	private final MovieOutboxScheduler movieOutboxScheduler;

	public MovieOutboxRelayScheduleStep(MovieOutboxScheduler movieOutboxScheduler) {
		this.movieOutboxScheduler = movieOutboxScheduler;
	}

	@Retryable
	public UUID execute(UUID outboxId) {
		try {
			var jobId = movieOutboxScheduler.schedule(new MovieOutboxProcessJobRequest(outboxId));
			return jobId.asUUID();
		}
		catch (Exception e) {
			log.error("Error while scheduling outbox: {} - {}", outboxId, e.getMessage(), e);
			throw e;
		}
	}

}
