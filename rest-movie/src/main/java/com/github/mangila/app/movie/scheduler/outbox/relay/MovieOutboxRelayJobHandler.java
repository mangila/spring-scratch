package com.github.mangila.app.movie.scheduler.outbox.relay;

import com.github.mangila.app.movie.scheduler.outbox.relay.step.*;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.server.runner.ThreadLocalJobContext;
import org.jobrunr.utils.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.UUID;

@Component
public class MovieOutboxRelayJobHandler implements JobRequestHandler<MovieOutboxRelayJobRequest> {

    private static final Logger log = new JobRunrDashboardLogger(
            LoggerFactory.getLogger(MovieOutboxRelayJobHandler.class));

    private final ClaimBatchStepHandler claimBatchStepHandler;
    private final ClaimVersionStepHandler claimVersionStepHandler;
    private final CreateDestinationStepHandler createDestinationStepHandler;
    private final FetchPayloadStepHandler fetchPayloadStepHandler;
    private final ScheduleDestinationStepHandler scheduleDestinationStepHandler;
    private final UpdateVersionStepHandler updateVersionStepHandler;

    public MovieOutboxRelayJobHandler(ClaimBatchStepHandler claimBatchStepHandler,
                                      ClaimVersionStepHandler claimVersionStepHandler,
                                      CreateDestinationStepHandler createDestinationStepHandler,
                                      FetchPayloadStepHandler fetchPayloadStepHandler,
                                      ScheduleDestinationStepHandler scheduleDestinationStepHandler,
                                      UpdateVersionStepHandler updateVersionStepHandler) {
        this.claimBatchStepHandler = claimBatchStepHandler;
        this.claimVersionStepHandler = claimVersionStepHandler;
        this.createDestinationStepHandler = createDestinationStepHandler;
        this.fetchPayloadStepHandler = fetchPayloadStepHandler;
        this.scheduleDestinationStepHandler = scheduleDestinationStepHandler;
        this.updateVersionStepHandler = updateVersionStepHandler;
    }

    @Override
    public void run(MovieOutboxRelayJobRequest jobRequest) throws Exception {
        final var context = ThreadLocalJobContext.getJobContext();
        final var limit = jobRequest.limit();
        final ClaimBatchStepResult claimBatchStepResult = context.runStepOnce("claimBatch", () -> claimBatchStepHandler.handle(limit));
        var outboxProjections = claimBatchStepResult.outboxProjections();
        log.info("Movie outbox relay size: {}", outboxProjections.size());
        var errs = new ArrayList<UUID>(limit);
        for (var outbox : outboxProjections) {
            final var outboxId = outbox.id();
            final var historyId = outbox.historyId();
            try {
                final boolean versionOk = context.runStepOnce("claimVersion:" + outboxId, () -> claimVersionStepHandler.handle(outbox));
                if (!versionOk) {
                    log.warn("Version mismatch for outbox id: {}", outboxId);
                    errs.add(outboxId);
                    continue;
                }
                final CreateDestinationStepResult createDestinationStepResult = context.runStepOnce("destination:" + outboxId, () -> createDestinationStepHandler.handle(outboxId));
                final FetchPayloadStepResult fetchPayloadStepResult = context.runStepOnce("payload:" + outboxId, () -> fetchPayloadStepHandler.handle(historyId));
                final var payload = fetchPayloadStepResult.projection()
                        .payload();
                for (var destinationEntity : createDestinationStepResult.destinationEntities()) {
                    final var destination = destinationEntity.getDestination();
                    context.runStepOnce(destination.toString() + ":" + outboxId, () -> {
                        var _ = scheduleDestinationStepHandler.handle(destinationEntity.getId(), payload, destination);
                    });
                }
                context.runStepOnce("updateVersion:" + outboxId, () -> updateVersionStepHandler.handle(outbox));
            } catch (Exception e) {
                log.error("Error processing outbox id: {}", outboxId, e);
                errs.add(outboxId);
            }
        }
        if (CollectionUtils.isNotNullOrEmpty(errs)) {
            throw new RuntimeException("Errors processing outbox ids: " + errs);
        }
    }

}
