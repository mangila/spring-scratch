package com.github.mangila.app.actor.outbox.destination.kafka;

import org.jobrunr.jobs.lambdas.JobRequest;

public record ActorKafkaDestinationJobRequest(java.util.UUID destinationId,
											  com.github.mangila.app.shared.persistence.type.Destination destination) implements JobRequest {
	@Override
	public Class<ActorKafkaDestinationJobHandler> getJobRequestHandler() {
		return ActorKafkaDestinationJobHandler.class;
	}
}
