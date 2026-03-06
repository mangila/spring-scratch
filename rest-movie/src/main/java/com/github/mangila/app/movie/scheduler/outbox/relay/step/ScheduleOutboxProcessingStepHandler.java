package com.github.mangila.app.movie.scheduler.outbox.relay.step;

import com.github.mangila.app.movie.scheduler.MovieScheduler;
import com.github.mangila.app.movie.scheduler.outbox.process.MovieOutboxProcessJobRequest;
import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ScheduleOutboxProcessingStepHandler {

    private static final Logger log = new JobRunrDashboardLogger(
            LoggerFactory.getLogger(ScheduleOutboxProcessingStepHandler.class));

    private final MovieScheduler movieScheduler;

    public ScheduleOutboxProcessingStepHandler(MovieScheduler movieScheduler) {
        this.movieScheduler = movieScheduler;
    }

    @Retryable
    public JobId handle(UUID outboxId) {
        try {
            return movieScheduler.schedule(new MovieOutboxProcessJobRequest(outboxId));
        } catch (Exception e) {
            log.error("Error while scheduling outbox: {} - {}", outboxId, e.getMessage(), e);
            throw e;
        }
    }
}
