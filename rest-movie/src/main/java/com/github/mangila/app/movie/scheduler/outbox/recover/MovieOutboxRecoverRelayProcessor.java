package com.github.mangila.app.movie.scheduler.outbox.recover;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
public class RecoverRelayProcessor {

    private static final Logger log = new JobRunrDashboardLogger(
            LoggerFactory.getLogger(RecoverRelayProcessor.class));

    @Retryable
    public void process(Job job) {
        log.info("Job has errors: {}", job.getMetadata().get("errors"));
    }
}
