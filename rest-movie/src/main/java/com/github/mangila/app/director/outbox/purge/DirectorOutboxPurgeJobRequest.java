package com.github.mangila.app.director.outbox.purge;

import org.jobrunr.jobs.lambdas.JobRequest;

public record DirectorOutboxPurgeJobRequest(int limit) implements JobRequest {

	@Override
	public Class<DirectorOutboxPurgeJobHandler> getJobRequestHandler() {
		return DirectorOutboxPurgeJobHandler.class;
	}
}
