package com.github.mangila.app.director.scheduler.outbox.producer;

import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.springframework.stereotype.Component;

@Component
public class DirectorOutboxProduceJobHandler implements JobRequestHandler<DirectorOutboxProduceJobRequest> {

	@Override
	public void run(DirectorOutboxProduceJobRequest jobRequest) throws Exception {

	}

}
