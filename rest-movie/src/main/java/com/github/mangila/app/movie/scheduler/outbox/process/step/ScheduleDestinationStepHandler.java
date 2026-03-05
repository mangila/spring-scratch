package com.github.mangila.app.movie.scheduler.outbox.process.step;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.mangila.app.movie.scheduler.MovieScheduler;
import com.github.mangila.app.movie.scheduler.outbox.destination.http.MovieHttpDestinationJobRequest;
import com.github.mangila.app.movie.scheduler.outbox.destination.kafka.MovieKafkaDestinationJobRequest;
import com.github.mangila.app.shared.persistence.type.Destination;
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

    @Retryable(excludes = IllegalStateException.class)
    public JobId handle(UUID destinationId, JsonNode payload, Destination destination) {
        try {
            return switch (destination) {
                case HTTP ->
                        movieScheduler.schedule(new MovieHttpDestinationJobRequest(destinationId, payload, destination));
                case KAFKA ->
                        movieScheduler.schedule(new MovieKafkaDestinationJobRequest(destinationId, payload, destination));
                default -> throw new IllegalStateException("Not supported destination: %s".formatted(destination));
            };
        } catch (Exception e) {
            log.error("Error scheduling destinationId: {} to destination: {} - {}", destinationId, destination, e.getMessage(), e);
            throw e;
        }
    }
}
