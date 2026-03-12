package com.github.mangila.app.director.outbox.destination.http;

import org.jobrunr.jobs.lambdas.JobRequest;

public record DirectorHttpDestinationJobRequest(java.util.UUID destinationId,
		com.github.mangila.app.shared.persistence.type.Destination destination) implements JobRequest {
	@Override
	public Class<DirectorHttpDestinationJobHandler> getJobRequestHandler() {
		return DirectorHttpDestinationJobHandler.class;
	}
}
