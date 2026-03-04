package com.github.mangila.app.movie.scheduler.outbox.consumer.destination.http;

import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.server.runner.ThreadLocalJobContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MovieHttpDestinationJobHandler implements JobRequestHandler<MovieHttpDestinationJobRequest> {

	private static final Logger log = new JobRunrDashboardLogger(
			LoggerFactory.getLogger(MovieHttpDestinationJobHandler.class));

	@Override
	public void run(MovieHttpDestinationJobRequest jobRequest) throws Exception {
		final var context = ThreadLocalJobContext.getJobContext();
	}

}
