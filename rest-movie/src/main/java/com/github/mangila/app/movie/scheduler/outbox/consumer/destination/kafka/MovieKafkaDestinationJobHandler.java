package com.github.mangila.app.movie.scheduler.outbox.consumer.destination.kafka;

import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.server.runner.ThreadLocalJobContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MovieKafkaDestinationJobHandler implements JobRequestHandler<MovieKafkaDestinationJobRequest> {

	private static final Logger log = new JobRunrDashboardLogger(
			LoggerFactory.getLogger(MovieKafkaDestinationJobHandler.class));

	@Override
	public void run(MovieKafkaDestinationJobRequest jobRequest) throws Exception {
		final var context = ThreadLocalJobContext.getJobContext();
	}

}
