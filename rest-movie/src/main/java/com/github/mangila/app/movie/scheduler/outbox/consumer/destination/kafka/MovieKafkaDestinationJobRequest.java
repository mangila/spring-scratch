package com.github.mangila.app.movie.scheduler.outbox.consumer.destination.kafka;

import org.jobrunr.jobs.lambdas.JobRequest;

public record MovieKafkaDestinationJobRequest(com.fasterxml.jackson.databind.JsonNode payload) implements JobRequest {
	@Override
	public Class<MovieKafkaDestinationJobHandler> getJobRequestHandler() {
		return MovieKafkaDestinationJobHandler.class;
	}
}
