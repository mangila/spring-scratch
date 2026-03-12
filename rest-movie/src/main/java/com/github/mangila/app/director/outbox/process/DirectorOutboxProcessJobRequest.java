package com.github.mangila.app.director.outbox.process;

import org.jobrunr.jobs.lambdas.JobRequest;

import java.util.UUID;

public record DirectorOutboxProcessJobRequest(UUID outboxId) implements JobRequest {

	@Override
	public Class<DirectorOutboxProcessJobHandler> getJobRequestHandler() {
		return DirectorOutboxProcessJobHandler.class;
	}
}
