package com.github.mangila.app.movie.scheduler.outbox.relay.step;

import com.github.mangila.app.movie.service.MovieOutboxVersionService;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CanProcessStepHandler {

    private static final Logger log = new JobRunrDashboardLogger(
            LoggerFactory.getLogger(CanProcessStepHandler.class));

    private final MovieOutboxVersionService movieOutboxVersionService;

    public CanProcessStepHandler(MovieOutboxVersionService movieOutboxVersionService) {
        this.movieOutboxVersionService = movieOutboxVersionService;
    }

    @Retryable
    public boolean handle(UUID aggregateId, int version) {
        try {
            return movieOutboxVersionService.canProcess(aggregateId, version);
        } catch (Exception e) {
            log.error("Error while checking if outbox can be processed: {}", e.getMessage(), e);
            throw e;
        }
    }

}
