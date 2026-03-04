package com.github.mangila.app.movie.scheduler.outbox.relay;

import com.github.mangila.app.movie.persistance.projection.MovieOutboxProjection;
import com.github.mangila.app.movie.scheduler.MovieScheduler;
import com.github.mangila.app.movie.scheduler.outbox.producer.MovieOutboxProduceJobRequest;
import com.github.mangila.app.movie.service.MovieOutboxService;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.server.runner.ThreadLocalJobContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Objects;

@Component
public class MovieOutboxRelayJobHandler implements JobRequestHandler<MovieOutboxRelayJobRequest> {

    private static final Logger log = new JobRunrDashboardLogger(LoggerFactory.getLogger(MovieOutboxRelayJobHandler.class));

    private final TransactionTemplate transactionTemplate;
    private final MovieOutboxService movieOutboxService;
    private final MovieScheduler movieScheduler;

    public MovieOutboxRelayJobHandler(TransactionTemplate transactionTemplate,
                                      MovieOutboxService movieOutboxService,
                                      MovieScheduler movieScheduler) {
        this.transactionTemplate = transactionTemplate;
        this.movieOutboxService = movieOutboxService;
        this.movieScheduler = movieScheduler;
    }

    @Override
    public void run(MovieOutboxRelayJobRequest jobRequest) throws Exception {
        final var context = ThreadLocalJobContext.getJobContext();
        final var limit = jobRequest.limit();
        var step = context.runStepOnce("claimPending", () -> {
            var l = transactionTemplate.execute(_ -> movieOutboxService.claimPending(limit));
            Objects.requireNonNull(l, "claimPending returned null");
            return new ClaimStep(l);
        });
        var outboxProjections = step.outboxProjections();
        log.info("{}", outboxProjections.size());
        for (var outbox : outboxProjections) {
            log.info("{}", outbox);
            final var idAsString = outbox.id().toString();
            context.runStepOnce(idAsString, () -> {
                movieScheduler.schedule(new MovieOutboxProduceJobRequest(outbox));
            });
        }
    }

    private record ClaimStep(List<MovieOutboxProjection> outboxProjections) implements JobContext.StepResult {

    }
}
