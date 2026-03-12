package com.github.mangila.app.movie.outbox.process;

import com.github.mangila.app.movie.outbox.process.step.MovieOutboxProcessDestinationStep;
import com.github.mangila.app.movie.outbox.process.step.MovieOutboxProcessScheduleStep;
import com.github.mangila.app.movie.outbox.process.step.MovieOutboxProcessStatusStep;
import com.github.mangila.app.movie.service.MovieOutboxService;
import com.github.mangila.app.movie.service.MovieOutboxVersionService;
import com.github.mangila.app.shared.persistence.type.Status;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.server.runner.ThreadLocalJobContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MovieOutboxProcessJobHandler implements JobRequestHandler<MovieOutboxProcessJobRequest> {

	private static final Logger log = new JobRunrDashboardLogger(
			LoggerFactory.getLogger(MovieOutboxProcessJobHandler.class));

	private final MovieOutboxService movieOutboxService;

	private final MovieOutboxVersionService movieOutboxVersionService;

	private final MovieOutboxProcessStatusStep movieChangeOutboxStatusStep;

	private final MovieOutboxProcessDestinationStep movieOutboxProcessDestinationStep;

	private final MovieOutboxProcessScheduleStep movieOutboxProcessScheduleStep;

	public MovieOutboxProcessJobHandler(MovieOutboxService movieOutboxService,
			MovieOutboxVersionService movieOutboxVersionService,
			MovieOutboxProcessStatusStep movieChangeOutboxStatusStep,
			MovieOutboxProcessDestinationStep movieOutboxProcessDestinationStep,
			MovieOutboxProcessScheduleStep movieOutboxProcessScheduleStep) {
		this.movieOutboxService = movieOutboxService;
		this.movieOutboxVersionService = movieOutboxVersionService;
		this.movieChangeOutboxStatusStep = movieChangeOutboxStatusStep;
		this.movieOutboxProcessDestinationStep = movieOutboxProcessDestinationStep;
		this.movieOutboxProcessScheduleStep = movieOutboxProcessScheduleStep;
	}

	@Override
	public void run(MovieOutboxProcessJobRequest jobRequest) throws Exception {
		final var context = ThreadLocalJobContext.getJobContext();
		final var outboxId = jobRequest.outboxId();

		final var outbox = movieOutboxService.findById(outboxId);
		log.info("Fetched outbox: {}", outboxId);

		final boolean canProcess = movieOutboxVersionService.canProcess(outbox.aggregateId(),
				outbox.aggregateVersion());
		if (!canProcess) {
			throw new IllegalStateException("Version mismatch %s: %s - %s".formatted(outboxId,
					outbox.aggregateVersion(), outbox.aggregateId()));
		}

		context.runStepOnce("status", () -> {
			final var fromStatus = Status.SCHEDULED;
			final var toStatus = Status.PROCESSING;
			final boolean execute = movieChangeOutboxStatusStep.execute(outboxId, fromStatus, toStatus);
			if (!execute) {
				throw new IllegalStateException(
						"Outbox: %s failed to change status from %s to %s".formatted(outboxId, fromStatus, toStatus));
			}
			log.info("Changed status of outbox: {} from {} to {}", outboxId, fromStatus, toStatus);
		});

		context.runStepOnce("destination", () -> {
			var destinationIds = movieOutboxProcessDestinationStep.execute(outboxId);
			log.info("Created destinations for outbox: {} - {}", outboxId, destinationIds);
		});

		context.runStepOnce("schedule", () -> {
			var jobId = movieOutboxProcessScheduleStep.execute(outboxId);
			log.info("Scheduled destination orchestrator for outbox: {} - jobId: {}", outboxId, jobId);
		});
	}

}
