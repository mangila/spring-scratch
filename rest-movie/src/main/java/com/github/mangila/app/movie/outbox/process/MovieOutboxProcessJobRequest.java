package com.github.mangila.app.movie.outbox.process;

import org.jobrunr.jobs.lambdas.JobRequest;

import java.util.UUID;

public record MovieOutboxProcessJobRequest(UUID outboxId) implements JobRequest {

	@Override
	public Class<MovieOutboxProcessJobHandler> getJobRequestHandler() {
		return MovieOutboxProcessJobHandler.class;
	}
}
