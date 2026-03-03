package com.github.mangila.movie.scheduler.outbox.relay;

import com.github.mangila.movie.persistence.outbox.OutboxJdbcRepository;
import com.github.mangila.movie.persistence.outbox.projection.OutboxProjection;
import com.github.mangila.movie.scheduler.outbox.OutboxScheduler;
import com.github.mangila.movie.scheduler.outbox.produce.OutboxProduceJobRequest;
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
public class OutboxRelayJobHandler implements JobRequestHandler<OutboxRelayJobRequest> {

    private static final Logger log = new JobRunrDashboardLogger(LoggerFactory.getLogger(OutboxRelayJobHandler.class));

    private final OutboxScheduler outboxScheduler;
    private final OutboxJdbcRepository outboxJdbcRepository;
    private final TransactionTemplate transactionTemplate;

    public OutboxRelayJobHandler(OutboxScheduler outboxScheduler,
                                 OutboxJdbcRepository outboxJdbcRepository,
                                 TransactionTemplate transactionTemplate) {
        this.outboxScheduler = outboxScheduler;
        this.outboxJdbcRepository = outboxJdbcRepository;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void run(OutboxRelayJobRequest jobRequest) throws Exception {
        final var limit = jobRequest.limit();
        final var context = ThreadLocalJobContext.getJobContext();
        var stepSuccess = context.runStepOnce("claim", () -> {
            var claims = transactionTemplate.execute(_ -> outboxJdbcRepository.claimPending(limit));
            Objects.requireNonNull(claims);
            log.info("Claim success");
            return new StepSuccess(claims);
        });
        var projections = stepSuccess.projections();
        for (var outbox : projections) {
            log.debug("Try relay outbox: {}", outbox);
            context.runStepOnce(outbox.id().toString(), () -> {
                var jobId = outboxScheduler.schedule(new OutboxProduceJobRequest(outbox));
                log.info("Scheduled outbox produce job: {} - {}", jobId.asUUID(), outbox);
            });
        }
    }

    record StepSuccess(List<OutboxProjection> projections) implements JobContext.StepResult {

    }
}