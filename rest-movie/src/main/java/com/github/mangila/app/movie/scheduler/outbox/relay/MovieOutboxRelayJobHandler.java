package com.github.mangila.app.movie.scheduler.outbox.relay;

import com.github.mangila.app.movie.scheduler.MovieScheduler;
import com.github.mangila.app.movie.scheduler.outbox.process.MovieOutboxProcessJobRequest;
import com.github.mangila.app.movie.scheduler.outbox.relay.step.ClaimBatchStepHandler;
import com.github.mangila.app.movie.service.MovieOutboxVersionService;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.server.runner.ThreadLocalJobContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MovieOutboxRelayJobHandler implements JobRequestHandler<MovieOutboxRelayJobRequest> {

    private static final Logger log = new JobRunrDashboardLogger(
            LoggerFactory.getLogger(MovieOutboxRelayJobHandler.class));

    private final MovieOutboxVersionService movieOutboxVersionService;
    private final MovieScheduler movieScheduler;

    private final ClaimBatchStepHandler claimBatchStepHandler;

    public MovieOutboxRelayJobHandler(MovieOutboxVersionService movieOutboxVersionService,
                                      MovieScheduler movieScheduler,
                                      ClaimBatchStepHandler claimBatchStepHandler) {
        this.movieOutboxVersionService = movieOutboxVersionService;
        this.movieScheduler = movieScheduler;
        this.claimBatchStepHandler = claimBatchStepHandler;
    }

    @Override
    public void run(MovieOutboxRelayJobRequest jobRequest) throws Exception {
        final var limit = jobRequest.limit();
        final var context = ThreadLocalJobContext.getJobContext();
        var claimBatchStepResult = context.runStepOnce("claimBatch", () -> {
            log.info("Claiming outbox batch with limit: {}", limit);
            return claimBatchStepHandler.handle(limit);
        });
        var claimedOutboxBatch = claimBatchStepResult.result();
        for (var outbox : claimedOutboxBatch) {
            final var outboxId = outbox.id();
            context.runStepOnce("schedule:" + outboxId, () -> {
                final var aggregateId = outbox.aggregateId();
                final var aggregateVersion = outbox.aggregateVersion();
                final var canProcess = movieOutboxVersionService.canProcess(aggregateId, aggregateVersion);
                if (canProcess) {
                    log.info("Scheduling outbox: {} with version: {}", outboxId, aggregateVersion);
                    var _ = movieScheduler.schedule(new MovieOutboxProcessJobRequest(outbox));
                } else {
                    log.info("Outbox: {} with version: {} is not yet ready to be processed", outboxId, aggregateVersion);
                }
            });
        }
    }

}
