package com.github.mangila.app.director.outbox.destination.step;

import com.github.mangila.app.director.properties.DirectorProperties;
import com.github.mangila.app.director.outbox.DirectorOutboxScheduler;
import com.github.mangila.app.director.outbox.destination.http.DirectorHttpDestinationJobRequest;
import com.github.mangila.app.director.outbox.destination.kafka.DirectorKafkaDestinationJobRequest;
import com.github.mangila.app.shared.persistence.base.projection.OutboxDestinationProjection;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DirectorOutboxDestinationOrchestratorScheduleStep {

	private final DirectorOutboxScheduler directorOutboxScheduler;

	public DirectorOutboxDestinationOrchestratorScheduleStep(DirectorOutboxScheduler directorOutboxScheduler) {
		this.directorOutboxScheduler = directorOutboxScheduler;
	}

	@Retryable(excludes = IllegalStateException.class)
	public UUID execute(OutboxDestinationProjection outboxDestination) {
		final var destinationId = outboxDestination.id();
		final var destination = outboxDestination.destination();
		var jobId = switch (destination) {
			case HTTP ->
				directorOutboxScheduler.schedule(new DirectorHttpDestinationJobRequest(destinationId, destination));
			case KAFKA ->
				directorOutboxScheduler.schedule(new DirectorKafkaDestinationJobRequest(destinationId, destination));
			default -> throw new IllegalStateException("Unsupported destination: %s - supported are: %s"
				.formatted(destination, DirectorProperties.SUPPORTED_DESTINATIONS));
		};
		return jobId.asUUID();
	}

}