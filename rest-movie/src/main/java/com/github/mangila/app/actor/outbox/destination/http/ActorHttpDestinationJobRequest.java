package com.github.mangila.app.actor.outbox.destination.http;

import org.jobrunr.jobs.lambdas.JobRequest;

public record ActorHttpDestinationJobRequest(java.util.UUID destinationId,
											 com.github.mangila.app.shared.persistence.type.Destination destination) implements JobRequest {
	@Override
	public Class<ActorHttpDestinationJobHandler> getJobRequestHandler() {
		return ActorHttpDestinationJobHandler.class;
	}
}
