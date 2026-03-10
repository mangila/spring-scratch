package com.github.mangila.app.movie.outbox.recover;

import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.server.runner.ThreadLocalJobContext;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.navigation.AmountRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MovieOutboxRecoverJobHandler implements JobRequestHandler<MovieOutboxRecoverJobRequest> {

    private static final Logger log = new JobRunrDashboardLogger(
            LoggerFactory.getLogger(MovieOutboxRecoverJobHandler.class));

    private final MovieOutboxRecoverRelayProcessor movieOutboxRecoverRelayProcessor;
    private final StorageProvider storageProvider;

    public MovieOutboxRecoverJobHandler(MovieOutboxRecoverRelayProcessor movieOutboxRecoverRelayProcessor,
                                        StorageProvider storageProvider) {
        this.movieOutboxRecoverRelayProcessor = movieOutboxRecoverRelayProcessor;
        this.storageProvider = storageProvider;
    }

    @Override
    public void run(MovieOutboxRecoverJobRequest jobRequest) throws Exception {
        final var context = ThreadLocalJobContext.getJobContext();
        final var limit = jobRequest.limit();

        final long hasFailedJobs = storageProvider.countJobs(StateName.FAILED);

        if (hasFailedJobs == 0) {
            log.info("No failed jobs found");
            return;
        }

        log.info("Found {} failed jobs", hasFailedJobs);

        var jobs = storageProvider.getJobList(StateName.FAILED, new AmountRequest("", limit));

        for (var job : jobs) {
            log.info("Job: {}", job.getId());

            var isRecurringJob = job.getRecurringJobId()
                    .isPresent();

            if (isRecurringJob) {
                var isRelay = job.getLabels()
                        .stream()
                        .anyMatch(label -> label.equals("relay"));
                if (isRelay) {
                    movieOutboxRecoverRelayProcessor.process(job);
                }
            }
        }

    }
}
