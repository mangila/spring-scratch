package com.github.mangila.app.actor.outbox.purge;

import org.jobrunr.jobs.lambdas.JobRequest;

public record ActorOutboxPurgeJobRequest(int limit) implements JobRequest {

	@Override
	public Class<ActorOutboxPurgeJobHandler> getJobRequestHandler() {
		return ActorOutboxPurgeJobHandler.class;
	}
}
