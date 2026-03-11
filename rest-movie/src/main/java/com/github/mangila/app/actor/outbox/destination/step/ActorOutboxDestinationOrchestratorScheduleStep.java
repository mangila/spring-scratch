package com.github.mangila.app.actor.outbox.destination.step;

import com.github.mangila.app.actor.outbox.ActorOutboxScheduler;
import com.github.mangila.app.actor.outbox.destination.http.ActorHttpDestinationJobRequest;
import com.github.mangila.app.actor.outbox.destination.kafka.ActorKafkaDestinationJobRequest;
import com.github.mangila.app.director.properties.DirectorProperties;
import com.github.mangila.app.shared.persistence.base.projection.OutboxDestinationProjection;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ActorOutboxDestinationOrchestratorScheduleStep {

    private final ActorOutboxScheduler actorOutboxScheduler;

    public ActorOutboxDestinationOrchestratorScheduleStep(ActorOutboxScheduler actorOutboxScheduler) {
        this.actorOutboxScheduler = actorOutboxScheduler;
    }

    @Retryable(excludes = IllegalStateException.class)
    public UUID execute(OutboxDestinationProjection outboxDestination) {
        final var destinationId = outboxDestination.id();
        final var destination = outboxDestination.destination();
        var jobId = switch (destination) {
            case HTTP -> actorOutboxScheduler.schedule(new ActorHttpDestinationJobRequest(destinationId, destination));
            case KAFKA ->
                    actorOutboxScheduler.schedule(new ActorKafkaDestinationJobRequest(destinationId, destination));
            default ->
                    throw new IllegalStateException("Unsupported destination: %s - supported are: %s".formatted(destination, DirectorProperties.SUPPORTED_DESTINATIONS));
        };
        return jobId.asUUID();
    }
}