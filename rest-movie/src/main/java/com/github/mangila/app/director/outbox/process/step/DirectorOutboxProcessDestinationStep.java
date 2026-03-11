package com.github.mangila.app.director.outbox.process.step;

import com.github.mangila.app.director.service.DirectorOutboxDestinationService;
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
public class DirectorOutboxProcessDestinationStep {

    private static final Logger log = new JobRunrDashboardLogger(
            LoggerFactory.getLogger(DirectorOutboxProcessDestinationStep.class));

    private final TransactionTemplate transactionTemplate;
    private final DirectorOutboxDestinationService destinationService;

    public DirectorOutboxProcessDestinationStep(TransactionTemplate transactionTemplate,
                                                DirectorOutboxDestinationService destinationService) {
        this.transactionTemplate = transactionTemplate;
        this.destinationService = destinationService;
    }

    @Retryable
    public List<UUID> execute(UUID outboxId) {
        try {
            var destinationEntities = transactionTemplate.execute(_ -> destinationService.createDestinations(outboxId));
            Objects.requireNonNull(destinationEntities, "destinationEntities returned null");
            return destinationEntities.stream()
                    .map(OutboxDestinationBaseEntity::getId)
                    .toList();
        } catch (Exception e) {
            log.error("Error while creating destinations for outbox: {} - {}", outboxId, e.getMessage(), e);
            throw e;
        }
    }

}
