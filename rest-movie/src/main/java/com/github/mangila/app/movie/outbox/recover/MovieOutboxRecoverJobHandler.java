package com.github.mangila.app.movie.outbox.recover;

import com.github.mangila.app.movie.outbox.recover.process.MovieOutboxRecoverPurgeProcessor;
import com.github.mangila.app.movie.outbox.recover.process.MovieOutboxRecoverRelayProcessor;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.server.runner.ThreadLocalJobContext;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.navigation.AmountRequest;
import org.jobrunr.utils.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MovieOutboxRecoverJobHandler implements JobRequestHandler<MovieOutboxRecoverJobRequest> {

    private static final Logger log = new JobRunrDashboardLogger(
            LoggerFactory.getLogger(MovieOutboxRecoverJobHandler.class));

    private final MovieOutboxRecoverRelayProcessor movieOutboxRecoverRelayProcessor;
    private final MovieOutboxRecoverPurgeProcessor movieOutboxRecoverPurgeProcessor;
    private final StorageProvider storageProvider;

    public MovieOutboxRecoverJobHandler(MovieOutboxRecoverRelayProcessor movieOutboxRecoverRelayProcessor,
                                        MovieOutboxRecoverPurgeProcessor movieOutboxRecoverPurgeProcessor,
                                        StorageProvider storageProvider) {
        this.movieOutboxRecoverRelayProcessor = movieOutboxRecoverRelayProcessor;
        this.movieOutboxRecoverPurgeProcessor = movieOutboxRecoverPurgeProcessor;
        this.storageProvider = storageProvider;
    }

    @Override
    public void run(MovieOutboxRecoverJobRequest jobRequest) throws Exception {
        final var context = ThreadLocalJobContext.getJobContext();
        final var limit = jobRequest.limit();

        var jobs = storageProvider.getJobList(StateName.FAILED, new AmountRequest("", limit));

        for (var job : jobs) {
            log.info("Job: {}", job.getId());

            var labels = job.getLabels();

            if (CollectionUtils.isNullOrEmpty(labels)) {
                log.info("No labels found for job");
                continue;
            }

            var isMovieDomain = matchLabel(labels, "movie");
            var isOutbox = matchLabel(labels, "outbox");

            if (isMovieDomain && isOutbox) {
                var isRelay = matchLabel(labels, "relay");
                if (isRelay) {
                    movieOutboxRecoverRelayProcessor.process(job);
                }
                var isPurge = matchLabel(labels, "purge");
                if (isPurge) {
                    movieOutboxRecoverPurgeProcessor.process(job);
                }
            }
        }
    }

    public static boolean matchLabel(List<String> labels, String label) {
        return labels.stream()
                .anyMatch(l -> l.equals(label));
    }
}
