package com.github.mangila.app.movie.outbox.destination.step;

import com.github.mangila.app.movie.service.MovieOutboxDestinationService;
import com.github.mangila.app.shared.persistence.base.projection.OutboxDestinationProjection;
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
public class ClaimOutboxDestinationStepHandler {

    private static final Logger log = new JobRunrDashboardLogger(
            LoggerFactory.getLogger(ClaimOutboxDestinationStepHandler.class));

    private final TransactionTemplate transactionTemplate;
    private final JsonMapper jsonMapper;
    private final MovieOutboxDestinationService movieOutboxDestinationService;

    public ClaimOutboxDestinationStepHandler(TransactionTemplate transactionTemplate,
                                             JsonMapper jsonMapper,
                                             MovieOutboxDestinationService movieOutboxDestinationService) {
        this.transactionTemplate = transactionTemplate;
        this.jsonMapper = jsonMapper;
        this.movieOutboxDestinationService = movieOutboxDestinationService;
    }

    @Retryable
    public String execute(UUID outboxId, Status from, Status to) {
        try {
            List<OutboxDestinationProjection> outboxIds = transactionTemplate.execute(_ -> movieOutboxDestinationService.claimBatch(outboxId, from, to));
            Objects.requireNonNull(outboxIds, "outboxIds returned null");
            return jsonMapper.writeValueAsString(outboxIds);
        } catch (Exception e) {
            log.error("Error while claiming outbox batch: {}", e.getMessage(), e);
            throw e;
        }
    }
}
