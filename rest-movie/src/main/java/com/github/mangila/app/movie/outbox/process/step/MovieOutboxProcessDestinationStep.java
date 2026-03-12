package com.github.mangila.app.movie.outbox.process.step;

import com.github.mangila.app.movie.service.MovieOutboxDestinationService;
import com.github.mangila.app.shared.persistence.base.OutboxDestinationBaseEntity;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class MovieOutboxProcessDestinationStep {

	private static final Logger log = new JobRunrDashboardLogger(
			LoggerFactory.getLogger(MovieOutboxProcessDestinationStep.class));

	private final TransactionTemplate transactionTemplate;

	private final MovieOutboxDestinationService destinationService;

	public MovieOutboxProcessDestinationStep(TransactionTemplate transactionTemplate,
			MovieOutboxDestinationService destinationService) {
		this.transactionTemplate = transactionTemplate;
		this.destinationService = destinationService;
	}

	@Retryable
	public List<UUID> execute(UUID outboxId) {
		try {
			var destinationEntities = transactionTemplate.execute(_ -> destinationService.createDestinations(outboxId));
			Objects.requireNonNull(destinationEntities, "destinationEntities returned null");
			return destinationEntities.stream().map(OutboxDestinationBaseEntity::getId).toList();
		}
		catch (Exception e) {
			log.error("Error while creating destinations for outbox: {} - {}", outboxId, e.getMessage(), e);
			throw e;
		}
	}

}
