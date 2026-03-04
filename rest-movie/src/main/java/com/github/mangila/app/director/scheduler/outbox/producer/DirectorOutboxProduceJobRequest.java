package com.github.mangila.app.director.scheduler.outbox.producer;

import org.jobrunr.jobs.lambdas.JobRequest;

public class DirectorOutboxProduceJobRequest implements JobRequest {

	@Override
	public Class<DirectorOutboxProduceJobHandler> getJobRequestHandler() {
		return DirectorOutboxProduceJobHandler.class;
	}

}
