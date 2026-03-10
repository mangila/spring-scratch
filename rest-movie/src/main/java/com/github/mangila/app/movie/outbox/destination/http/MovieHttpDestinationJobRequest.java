package com.github.mangila.app.movie.outbox.destination.http;

import org.jobrunr.jobs.lambdas.JobRequest;

public record MovieHttpDestinationJobRequest(java.util.UUID destinationId,
											 com.github.mangila.app.shared.persistence.type.Destination destination) implements JobRequest {
	@Override
	public Class<MovieHttpDestinationJobHandler> getJobRequestHandler() {
		return MovieHttpDestinationJobHandler.class;
	}
}
