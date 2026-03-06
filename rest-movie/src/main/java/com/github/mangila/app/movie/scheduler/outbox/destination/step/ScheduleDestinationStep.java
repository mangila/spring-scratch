package com.github.mangila.app.movie.scheduler.outbox.destination.step;

import com.github.mangila.app.movie.properties.MovieProperties;
import com.github.mangila.app.movie.scheduler.MovieScheduler;
import com.github.mangila.app.movie.scheduler.outbox.destination.http.MovieHttpDestinationJobRequest;
import com.github.mangila.app.movie.scheduler.outbox.destination.kafka.MovieKafkaDestinationJobRequest;
import com.github.mangila.app.shared.persistence.base.projection.OutboxDestinationProjection;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ScheduleDestinationStep {

    private final MovieScheduler movieScheduler;

    public ScheduleDestinationStep(MovieScheduler movieScheduler) {
        this.movieScheduler = movieScheduler;
    }

    @Retryable(excludes = IllegalStateException.class)
    public UUID execute(OutboxDestinationProjection outboxDestination) {
        final var destinationId = outboxDestination.id();
        final var destination = outboxDestination.destination();
        var jobId = switch (destination) {
            case HTTP -> movieScheduler.schedule(new MovieHttpDestinationJobRequest(destinationId,destination));
            case KAFKA -> movieScheduler.schedule(new MovieKafkaDestinationJobRequest(destinationId,destination));
            default ->
                    throw new IllegalStateException("Unsupported destination: %s - supported are: %s".formatted(destination, MovieProperties.SUPPORTED_DESTINATIONS));
        };
        return jobId.asUUID();
    }
}