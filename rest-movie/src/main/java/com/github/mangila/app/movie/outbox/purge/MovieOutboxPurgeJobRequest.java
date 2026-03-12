package com.github.mangila.app.movie.outbox.purge;

import org.jobrunr.jobs.lambdas.JobRequest;

public record MovieOutboxPurgeJobRequest(int limit) implements JobRequest {

	@Override
	public Class<MovieOutboxPurgeJobHandler> getJobRequestHandler() {
		return MovieOutboxPurgeJobHandler.class;
	}
}
