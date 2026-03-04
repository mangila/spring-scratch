package com.github.mangila.app.actor.scheduler.outbox.producer;

import org.jobrunr.jobs.lambdas.JobRequest;

public class ActorOutboxProduceJobRequest implements JobRequest {

	@Override
	public Class<ActorOutboxProduceJobHandler> getJobRequestHandler() {
		return ActorOutboxProduceJobHandler.class;
	}

}
