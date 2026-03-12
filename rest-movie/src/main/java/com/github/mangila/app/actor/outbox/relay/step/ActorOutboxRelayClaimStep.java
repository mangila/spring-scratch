package com.github.mangila.app.actor.outbox.relay.step;

import com.github.mangila.app.actor.service.ActorOutboxService;
import com.github.mangila.app.shared.persistence.type.Status;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class ActorOutboxRelayClaimStep {

	private static final Logger log = new JobRunrDashboardLogger(
			LoggerFactory.getLogger(ActorOutboxRelayClaimStep.class));

	private final ActorOutboxService actorOutboxService;

	private final JsonMapper jsonMapper;

	private final TransactionTemplate transactionTemplate;

	public ActorOutboxRelayClaimStep(ActorOutboxService actorOutboxService, JsonMapper jsonMapper,
			TransactionTemplate transactionTemplate) {
		this.actorOutboxService = actorOutboxService;
		this.jsonMapper = jsonMapper;
		this.transactionTemplate = transactionTemplate;
	}

	@Retryable
	public String execute(Status from, Status to, int limit) {
		try {
			List<UUID> outboxIds = transactionTemplate.execute(_ -> actorOutboxService.claimBatch(from, to, limit));
			Objects.requireNonNull(outboxIds, "outboxIds returned null");
			return jsonMapper.writeValueAsString(outboxIds);
		}
		catch (Exception e) {
			log.error("Error while claiming outbox batch: {}", e.getMessage(), e);
			throw e;
		}
	}

}
