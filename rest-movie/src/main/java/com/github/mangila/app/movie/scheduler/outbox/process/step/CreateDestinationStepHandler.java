package com.github.mangila.app.movie.scheduler.outbox.process.step;

import com.github.mangila.app.movie.service.MovieOutboxDestinationService;
import com.github.mangila.app.shared.persistence.base.OutboxDestinationBaseEntity;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class CreateDestinationStepHandler {

    private static final Logger log = new JobRunrDashboardLogger(
            LoggerFactory.getLogger(CreateDestinationStepHandler.class));

    private final MovieOutboxDestinationService destinationService;

    public CreateDestinationStepHandler(MovieOutboxDestinationService destinationService) {
        this.destinationService = destinationService;
    }

    @Retryable
    public List<UUID> handle(UUID outboxId) {
        try {
            var destinationEntities = destinationService.createDestinations(outboxId);
            return destinationEntities.stream()
                    .map(OutboxDestinationBaseEntity::getId)
                    .toList();
        } catch (Exception e) {
            log.error("Error while creating destinations for outbox: {} - {}", outboxId, e.getMessage(), e);
            throw e;
        }
    }

}
