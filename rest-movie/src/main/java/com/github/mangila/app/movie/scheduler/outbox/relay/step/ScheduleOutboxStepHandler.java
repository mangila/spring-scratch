package com.github.mangila.app.movie.scheduler.outbox.relay.step;

import com.github.mangila.app.movie.scheduler.MovieScheduler;
import com.github.mangila.app.movie.scheduler.outbox.process.MovieOutboxProcessJobRequest;
import com.github.mangila.app.shared.persistence.base.projection.OutboxProjection;
import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
public class ScheduleOutboxStepHandler {

    private static final Logger log = new JobRunrDashboardLogger(
            LoggerFactory.getLogger(ScheduleOutboxStepHandler.class));

    private final MovieScheduler movieScheduler;

    public ScheduleOutboxStepHandler(MovieScheduler movieScheduler) {
        this.movieScheduler = movieScheduler;
    }

    @Retryable
    public JobId handle(OutboxProjection outbox) {
        try {
            return movieScheduler.schedule(new MovieOutboxProcessJobRequest(outbox));
        } catch (Exception e) {
            log.error("Error while scheduling outbox: {} - {}", outbox.id(), e.getMessage(), e);
            throw e;
        }
    }
}
