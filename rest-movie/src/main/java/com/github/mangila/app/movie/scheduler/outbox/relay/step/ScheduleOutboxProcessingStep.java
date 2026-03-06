package com.github.mangila.app.movie.scheduler.outbox.relay.step;

import com.github.mangila.app.movie.scheduler.MovieScheduler;
import com.github.mangila.app.movie.scheduler.outbox.process.MovieOutboxProcessJobRequest;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ScheduleOutboxProcessingStep {

    private static final Logger log = new JobRunrDashboardLogger(
            LoggerFactory.getLogger(ScheduleOutboxProcessingStep.class));

    private final MovieScheduler movieScheduler;

    public ScheduleOutboxProcessingStep(MovieScheduler movieScheduler) {
        this.movieScheduler = movieScheduler;
    }

    @Retryable
    public UUID execute(UUID outboxId) {
        try {
            var jobId = movieScheduler.schedule(new MovieOutboxProcessJobRequest(outboxId));
            return jobId.asUUID();
        } catch (Exception e) {
            log.error("Error while scheduling outbox: {} - {}", outboxId, e.getMessage(), e);
            throw e;
        }
    }
}
