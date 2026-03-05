package com.github.mangila.app.movie.scheduler.outbox.destination;

import org.jobrunr.jobs.lambdas.JobRequest;

public record MovieOutboxDestinationMonitorJobRequest() implements JobRequest {

	@Override
	public Class<MovieOutboxDestinationMonitorJobHandler> getJobRequestHandler() {
		return MovieOutboxDestinationMonitorJobHandler.class;
	}
}
