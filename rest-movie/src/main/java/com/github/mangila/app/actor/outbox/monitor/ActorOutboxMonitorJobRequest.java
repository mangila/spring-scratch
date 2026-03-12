package com.github.mangila.app.actor.outbox.monitor;

import org.jobrunr.jobs.lambdas.JobRequest;

public record ActorOutboxMonitorJobRequest(int limit) implements JobRequest {

	@Override
	public Class<ActorOutboxMonitorJobHandler> getJobRequestHandler() {
		return ActorOutboxMonitorJobHandler.class;
	}
}
