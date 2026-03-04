package com.github.mangila.app.actor.scheduler.outbox.relay;

import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.springframework.stereotype.Component;

@Component
public class ActorOutboxRelayJobHandler implements JobRequestHandler<ActorOutboxRelayJobRequest> {

	@Override
	public void run(ActorOutboxRelayJobRequest jobRequest) throws Exception {

	}

}
