package com.github.mangila.app.director.outbox.relay;

import org.jobrunr.jobs.lambdas.JobRequest;

public record DirectorOutboxRelayJobRequest(int limit) implements JobRequest {

	@Override
	public Class<DirectorOutboxRelayJobHandler> getJobRequestHandler() {
		return DirectorOutboxRelayJobHandler.class;
	}
}
