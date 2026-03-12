package com.github.mangila.app.actor.outbox.recover;

import org.jobrunr.jobs.lambdas.JobRequest;

public record ActorOutboxRecoverJobRequest(int limit) implements JobRequest {

	@Override
	public Class<ActorOutboxRecoverJobHandler> getJobRequestHandler() {
		return ActorOutboxRecoverJobHandler.class;
	}
}
