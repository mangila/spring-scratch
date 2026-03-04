package com.github.mangila.app.movie.scheduler.outbox.consumer.destination.kafka;

import org.jobrunr.jobs.lambdas.JobRequest;

public record MovieKafkaDestinationJobRequest(java.util.UUID destinationId,
		com.fasterxml.jackson.databind.JsonNode payload,
		com.github.mangila.app.shared.persistence.type.Destination destination) implements JobRequest {
	@Override
	public Class<MovieKafkaDestinationJobHandler> getJobRequestHandler() {
		return MovieKafkaDestinationJobHandler.class;
	}
}
