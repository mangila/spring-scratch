package com.github.mangila.app.movie.scheduler.outbox.relay;

import com.github.mangila.app.movie.scheduler.outbox.relay.step.CanProcessStepHandler;
import com.github.mangila.app.movie.scheduler.outbox.relay.step.ScheduleOutboxStepHandler;
import com.github.mangila.app.movie.scheduler.outbox.shared.ClaimBatchStepHandler;
import com.github.mangila.app.shared.persistence.type.Status;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.server.runner.ThreadLocalJobContext;
import org.jobrunr.utils.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MovieOutboxRelayJobHandler implements JobRequestHandler<MovieOutboxRelayJobRequest> {

    private static final Logger log = new JobRunrDashboardLogger(
            LoggerFactory.getLogger(MovieOutboxRelayJobHandler.class));

    private final ClaimBatchStepHandler claimBatchStepHandler;
    private final CanProcessStepHandler canProcessStepHandler;
    private final ScheduleOutboxStepHandler scheduleOutboxStepHandler;

    public MovieOutboxRelayJobHandler(ClaimBatchStepHandler claimBatchStepHandler,
                                      CanProcessStepHandler canProcessStepHandler,
                                      ScheduleOutboxStepHandler scheduleOutboxStepHandler) {
        this.claimBatchStepHandler = claimBatchStepHandler;
        this.canProcessStepHandler = canProcessStepHandler;
        this.scheduleOutboxStepHandler = scheduleOutboxStepHandler;
    }

    @Override
    public void run(MovieOutboxRelayJobRequest jobRequest) throws Exception {
        final var limit = jobRequest.limit();
        final var context = ThreadLocalJobContext.getJobContext();
        var batch = context.runStepOnce("claimBatch", () -> {
            log.info("Claiming outbox batch with limit: {}", limit);
            return claimBatchStepHandler.handle(Status.PENDING, Status.CLAIMED, limit);
        });
        var batchResult = batch.result();
        if (CollectionUtils.isNullOrEmpty(batchResult)) {
            log.info("No outboxes to process");
            return;
        }
        for (var outbox : batchResult) {
            final var outboxId = outbox.id();
            final boolean canProcess = context.runStepOnce("canProcess:" + outboxId, () -> {
                log.info("Run step canProcess - {}", outboxId);
                return canProcessStepHandler.handle(outbox.aggregateId(), outbox.aggregateVersion());
            });
            if (canProcess) {
                context.runStepOnce("schedule:" + outboxId, () -> {
                    log.info("Run step schedule - {}", outboxId);
                    var _ = scheduleOutboxStepHandler.handle(outbox);
                });
                log.info("Scheduling outbox: {} with version: {}", outboxId, outbox.aggregateVersion());
            } else {
                log.info("Outbox: {} with version: {} is not yet ready to be processed", outboxId, outbox.aggregateVersion());
            }
        }
    }

}
