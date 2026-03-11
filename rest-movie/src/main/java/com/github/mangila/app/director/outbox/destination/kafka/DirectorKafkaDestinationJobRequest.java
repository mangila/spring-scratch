package com.github.mangila.app.director.outbox.destination.kafka;

import org.jobrunr.jobs.lambdas.JobRequest;

public record DirectorKafkaDestinationJobRequest(java.util.UUID destinationId,
											  com.github.mangila.app.shared.persistence.type.Destination destination) implements JobRequest {
	@Override
	public Class<DirectorKafkaDestinationJobHandler> getJobRequestHandler() {
		return DirectorKafkaDestinationJobHandler.class;
	}
}
