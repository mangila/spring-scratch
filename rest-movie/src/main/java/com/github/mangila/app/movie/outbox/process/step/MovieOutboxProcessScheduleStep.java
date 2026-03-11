package com.github.mangila.app.movie.outbox.process.step;

import com.github.mangila.app.movie.outbox.MovieOutboxScheduler;
import com.github.mangila.app.movie.outbox.destination.MovieOutboxDestinationOrchestratorJobRequest;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MovieOutboxProcessScheduleStep {

    private static final Logger log = new JobRunrDashboardLogger(
            LoggerFactory.getLogger(MovieOutboxProcessScheduleStep.class));

    private final MovieOutboxScheduler movieOutboxScheduler;

    public MovieOutboxProcessScheduleStep(MovieOutboxScheduler movieOutboxScheduler) {
        this.movieOutboxScheduler = movieOutboxScheduler;
    }

    @Retryable
    public UUID execute(UUID outboxId) {
        try {
            var jobId = movieOutboxScheduler.schedule(new MovieOutboxDestinationOrchestratorJobRequest(outboxId));
            return jobId.asUUID();
        } catch (Exception e) {
            log.error("Error scheduling outboxId: {} to orchestrator: {}", outboxId, e.getMessage(), e);
            throw e;
        }
    }
}
