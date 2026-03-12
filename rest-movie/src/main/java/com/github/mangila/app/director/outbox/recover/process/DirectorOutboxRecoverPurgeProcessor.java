package com.github.mangila.app.director.outbox.recover.process;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
public class DirectorOutboxRecoverPurgeProcessor {

	private static final Logger log = new JobRunrDashboardLogger(
			LoggerFactory.getLogger(DirectorOutboxRecoverPurgeProcessor.class));

	@Retryable
	public void process(Job job) {
		log.info("Job has errors: {}", job.getMetadata().get("errors"));
	}

}
