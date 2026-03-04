package com.github.mangila.app.director.scheduler.outbox.relay;

import org.jobrunr.jobs.lambdas.JobRequest;

public record DirectorOutboxRelayJobRequest(int limit) implements JobRequest {

	@Override
	public Class<DirectorOutboxRelayJobHandler> getJobRequestHandler() {
		return DirectorOutboxRelayJobHandler.class;
	}
}
