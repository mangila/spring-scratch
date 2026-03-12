package com.github.mangila.app.movie.outbox.destination.step;

import com.github.mangila.app.movie.properties.MovieProperties;
import com.github.mangila.app.movie.outbox.MovieOutboxScheduler;
import com.github.mangila.app.movie.outbox.destination.http.MovieHttpDestinationJobRequest;
import com.github.mangila.app.movie.outbox.destination.kafka.MovieKafkaDestinationJobRequest;
import com.github.mangila.app.shared.persistence.base.projection.OutboxDestinationProjection;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MovieOutboxDestinationOrchestratorScheduleStep {

	private final MovieOutboxScheduler movieOutboxScheduler;

	public MovieOutboxDestinationOrchestratorScheduleStep(MovieOutboxScheduler movieOutboxScheduler) {
		this.movieOutboxScheduler = movieOutboxScheduler;
	}

	@Retryable(excludes = IllegalStateException.class)
	public UUID execute(OutboxDestinationProjection outboxDestination) {
		final var destinationId = outboxDestination.id();
		final var destination = outboxDestination.destination();
		var jobId = switch (destination) {
			case HTTP -> movieOutboxScheduler.schedule(new MovieHttpDestinationJobRequest(destinationId, destination));
			case KAFKA ->
				movieOutboxScheduler.schedule(new MovieKafkaDestinationJobRequest(destinationId, destination));
			default -> throw new IllegalStateException("Unsupported destination: %s - supported are: %s"
				.formatted(destination, MovieProperties.SUPPORTED_DESTINATIONS));
		};
		return jobId.asUUID();
	}

}