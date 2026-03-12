package com.github.mangila.app.actor.outbox.monitor;

import com.github.mangila.app.actor.service.ActorOutboxDestinationService;
import com.github.mangila.app.actor.service.ActorOutboxService;
import com.github.mangila.app.actor.service.ActorOutboxVersionService;
import com.github.mangila.app.shared.persistence.type.Status;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.server.runner.ThreadLocalJobContext;
import org.jobrunr.utils.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.UUID;

@Component
public class ActorOutboxMonitorJobHandler implements JobRequestHandler<ActorOutboxMonitorJobRequest> {

	private static final Logger log = new JobRunrDashboardLogger(
			LoggerFactory.getLogger(ActorOutboxMonitorJobHandler.class));

	private final TransactionTemplate transactionTemplate;

	private final ActorOutboxVersionService actorOutboxVersionService;

	private final ActorOutboxService actorOutboxService;

	private final ActorOutboxDestinationService actorOutboxDestinationService;

	public ActorOutboxMonitorJobHandler(TransactionTemplate transactionTemplate,
			ActorOutboxVersionService actorOutboxVersionService, ActorOutboxService actorOutboxService,
			ActorOutboxDestinationService actorOutboxDestinationService) {
		this.transactionTemplate = transactionTemplate;
		this.actorOutboxVersionService = actorOutboxVersionService;
		this.actorOutboxService = actorOutboxService;
		this.actorOutboxDestinationService = actorOutboxDestinationService;
	}

	@Override
	public void run(ActorOutboxMonitorJobRequest jobRequest) throws Exception {
		final var context = ThreadLocalJobContext.getJobContext();
		final var limit = jobRequest.limit();

		var outboxes = actorOutboxService.findAllByStatus(Status.PROCESSING, limit);
		log.info("Found {} outboxes to monitor", outboxes.size());

		var errors = new ArrayList<UUID>(outboxes.size());
		for (var outbox : outboxes) {
			final var outboxId = outbox.id();
			final var aggregateId = outbox.aggregateId();
			try {
				var destinationEntities = actorOutboxDestinationService.findAllByOutboxId(outboxId);
				if (destinationEntities.isEmpty()) {
					log.info("No destinations for outbox: {}", outboxId);
					continue;
				}
				log.info("Found {} destinations for outbox: {}", destinationEntities.size(), outboxId);
				var allSuccessMatch = destinationEntities.stream()
					.allMatch(destinationEntity -> destinationEntity.status() == Status.SUCCESS);
				if (allSuccessMatch) {
					final var fromStatus = Status.PROCESSING;
					final var toStatus = Status.SUCCESS;
					transactionTemplate.executeWithoutResult(_ -> {
						final boolean ok = actorOutboxService.changeStatus(outboxId, fromStatus, toStatus);
						if (!ok) {
							throw new IllegalStateException("Outbox: %s failed to change status from %s to %s"
								.formatted(outboxId, fromStatus, toStatus));
						}
						actorOutboxVersionService.increment(aggregateId);
						log.info("Outbox: {} changed status from {} to {} and bumped version on aggregate: {}",
								outboxId, fromStatus, toStatus, aggregateId);
					});
				}
			}
			catch (Exception e) {
				log.error("Error while monitoring outbox: {} - {}", outboxId, e.getMessage(), e);
				errors.add(outboxId);
			}
		}

		if (CollectionUtils.isNotNullOrEmpty(errors)) {
			throw new IllegalStateException("Failed to monitor outboxes: %s".formatted(errors));
		}

	}

}
