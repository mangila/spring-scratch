package com.github.mangila.app.actor.outbox.process;

import com.github.mangila.app.actor.outbox.process.step.ActorOutboxProcessDestinationStep;
import com.github.mangila.app.actor.outbox.process.step.ActorOutboxProcessScheduleStep;
import com.github.mangila.app.actor.outbox.process.step.ActorOutboxProcessStatusStep;
import com.github.mangila.app.actor.service.ActorOutboxService;
import com.github.mangila.app.actor.service.ActorOutboxVersionService;
import com.github.mangila.app.shared.persistence.type.Status;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.server.runner.ThreadLocalJobContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ActorOutboxProcessJobHandler implements JobRequestHandler<ActorOutboxProcessJobRequest> {

	private static final Logger log = new JobRunrDashboardLogger(
			LoggerFactory.getLogger(ActorOutboxProcessJobHandler.class));

	private final ActorOutboxService actorOutboxService;

	private final ActorOutboxVersionService actorOutboxVersionService;

	private final ActorOutboxProcessStatusStep actorOutboxProcessStatusStep;

	private final ActorOutboxProcessDestinationStep actorOutboxProcessDestinationStep;

	private final ActorOutboxProcessScheduleStep actorOutboxProcessScheduleStep;

	public ActorOutboxProcessJobHandler(ActorOutboxService actorOutboxService,
			ActorOutboxVersionService actorOutboxVersionService,
			ActorOutboxProcessStatusStep actorOutboxProcessStatusStep,
			ActorOutboxProcessDestinationStep actorOutboxProcessDestinationStep,
			ActorOutboxProcessScheduleStep actorOutboxProcessScheduleStep) {
		this.actorOutboxService = actorOutboxService;
		this.actorOutboxVersionService = actorOutboxVersionService;
		this.actorOutboxProcessStatusStep = actorOutboxProcessStatusStep;
		this.actorOutboxProcessDestinationStep = actorOutboxProcessDestinationStep;
		this.actorOutboxProcessScheduleStep = actorOutboxProcessScheduleStep;
	}

	@Override
	public void run(ActorOutboxProcessJobRequest jobRequest) throws Exception {
		final var context = ThreadLocalJobContext.getJobContext();
		final var outboxId = jobRequest.outboxId();

		final var outbox = actorOutboxService.findById(outboxId);
		log.info("Fetched outbox: {}", outboxId);

		final boolean canProcess = actorOutboxVersionService.canProcess(outbox.aggregateId(),
				outbox.aggregateVersion());
		if (!canProcess) {
			throw new IllegalStateException("Version mismatch %s: %s - %s".formatted(outboxId,
					outbox.aggregateVersion(), outbox.aggregateId()));
		}

		context.runStepOnce("status", () -> {
			final var fromStatus = Status.SCHEDULED;
			final var toStatus = Status.PROCESSING;
			final boolean execute = actorOutboxProcessStatusStep.execute(outboxId, fromStatus, toStatus);
			if (!execute) {
				throw new IllegalStateException(
						"Outbox: %s failed to change status from %s to %s".formatted(outboxId, fromStatus, toStatus));
			}
			log.info("Changed status of outbox: {} from {} to {}", outboxId, fromStatus, toStatus);
		});

		context.runStepOnce("destination", () -> {
			var destinationIds = actorOutboxProcessDestinationStep.execute(outboxId);
			log.info("Created destinations for outbox: {} - {}", outboxId, destinationIds);
		});

		context.runStepOnce("schedule", () -> {
			var jobId = actorOutboxProcessScheduleStep.execute(outboxId);
			log.info("Scheduled destination orchestrator for outbox: {} - jobId: {}", outboxId, jobId);
		});
	}

}
