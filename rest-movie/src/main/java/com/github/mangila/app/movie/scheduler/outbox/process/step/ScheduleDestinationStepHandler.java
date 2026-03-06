package com.github.mangila.app.movie.scheduler.outbox.process.step;

import com.github.mangila.app.movie.scheduler.MovieScheduler;
import com.github.mangila.app.movie.scheduler.outbox.destination.MovieOutboxDestinationOrchestratorJobRequest;
import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ScheduleDestinationStepHandler {

    private static final Logger log = new JobRunrDashboardLogger(
            LoggerFactory.getLogger(ScheduleDestinationStepHandler.class));

    private final MovieScheduler movieScheduler;

    public ScheduleDestinationStepHandler(MovieScheduler movieScheduler) {
        this.movieScheduler = movieScheduler;
    }

    @Retryable
    public JobId handle(UUID outboxId) {
        try {
            return movieScheduler.schedule(new MovieOutboxDestinationOrchestratorJobRequest(outboxId));
        } catch (Exception e) {
            log.error("Error scheduling outboxId: {} to orchestrator: {} - {}", outboxId, e.getMessage(), e);
            throw e;
        }
    }
}
