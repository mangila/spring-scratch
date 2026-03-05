package com.github.mangila.app.movie.scheduler.outbox.purge.step;

import com.github.mangila.app.movie.scheduler.outbox.purge.step.result.FindOutboxDestinationStepResult;
import com.github.mangila.app.movie.service.MovieOutboxDestinationService;
import com.github.mangila.app.shared.persistence.type.Status;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class FindOutboxDestinationStepHandler {

    private static final Logger log = new JobRunrDashboardLogger(
            LoggerFactory.getLogger(FindOutboxDestinationStepHandler.class));

    private final MovieOutboxDestinationService destinationService;

    public FindOutboxDestinationStepHandler(MovieOutboxDestinationService destinationService) {
        this.destinationService = destinationService;
    }

    @Retryable
    public FindOutboxDestinationStepResult handle(UUID outboxId, Status status) {
        try {
            var destinations = destinationService.findAllByOutboxIdAndStatus(outboxId, status);
            return new FindOutboxDestinationStepResult(destinations);
        } catch (Exception e) {
            log.error("Error finding outbox destinations for outboxId: {}, status: {}", outboxId, status, e);
            throw e;
        }
    }
}
