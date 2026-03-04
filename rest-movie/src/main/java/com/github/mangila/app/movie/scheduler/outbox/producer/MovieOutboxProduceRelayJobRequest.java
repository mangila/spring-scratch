package com.github.mangila.app.movie.scheduler.outbox.producer;

import org.jobrunr.jobs.lambdas.JobRequest;

public record MovieOutboxProduceRelayJobRequest(int limit) implements JobRequest {

	@Override
	public Class<MovieOutboxProduceRelayJobHandler> getJobRequestHandler() {
		return MovieOutboxProduceRelayJobHandler.class;
	}
}
