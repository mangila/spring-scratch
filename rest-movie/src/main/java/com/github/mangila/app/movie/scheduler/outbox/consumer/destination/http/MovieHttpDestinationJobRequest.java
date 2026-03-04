package com.github.mangila.app.movie.scheduler.outbox.consumer.destination.http;

import org.jobrunr.jobs.lambdas.JobRequest;

public record MovieHttpDestinationJobRequest(java.util.UUID destinationId,
		com.fasterxml.jackson.databind.JsonNode payload,
		com.github.mangila.app.shared.persistence.type.Destination destination) implements JobRequest {
	@Override
	public Class<MovieHttpDestinationJobHandler> getJobRequestHandler() {
		return MovieHttpDestinationJobHandler.class;
	}
}
