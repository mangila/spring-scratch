package com.github.mangila.app.movie.scheduler.outbox.process;

import com.github.mangila.app.movie.scheduler.outbox.process.step.ChangeStatusStepHandler;
import com.github.mangila.app.movie.scheduler.outbox.process.step.CreateDestinationStepHandler;
import com.github.mangila.app.movie.scheduler.outbox.process.step.FetchPayloadStepHandler;
import com.github.mangila.app.movie.scheduler.outbox.process.step.ScheduleDestinationStepHandler;
import com.github.mangila.app.shared.persistence.type.Status;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.server.runner.ThreadLocalJobContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static com.github.mangila.app.shared.persistence.type.Status.*;
import static com.github.mangila.app.shared.persistence.type.Status.CLAIMED;

@Component
public class MovieOutboxProcessJobHandler implements JobRequestHandler<MovieOutboxProcessJobRequest> {

    private static final Logger log = new JobRunrDashboardLogger(
            LoggerFactory.getLogger(MovieOutboxProcessJobHandler.class));

    private final ChangeStatusStepHandler changeStatusStepHandler;
    private final CreateDestinationStepHandler createDestinationStepHandler;
    private final FetchPayloadStepHandler fetchPayloadStepHandler;
    private final ScheduleDestinationStepHandler scheduleDestinationStepHandler;

    public MovieOutboxProcessJobHandler(ChangeStatusStepHandler changeStatusStepHandler,
                                        CreateDestinationStepHandler createDestinationStepHandler,
                                        FetchPayloadStepHandler fetchPayloadStepHandler,
                                        ScheduleDestinationStepHandler scheduleDestinationStepHandler) {
        this.changeStatusStepHandler = changeStatusStepHandler;
        this.createDestinationStepHandler = createDestinationStepHandler;
        this.fetchPayloadStepHandler = fetchPayloadStepHandler;
        this.scheduleDestinationStepHandler = scheduleDestinationStepHandler;
    }

    @Override
    public void run(MovieOutboxProcessJobRequest jobRequest) throws Exception {
        final var context = ThreadLocalJobContext.getJobContext();
        final var outbox = jobRequest.outbox();
        final var outboxId = outbox.id();
        final var historyId = outbox.historyId();
        boolean ok = context.runStepOnce("changeStatus", () -> {
            log.info("Changing status of outbox: {} from {} to {}", outboxId, CLAIMED, PROCESSING);
            return changeStatusStepHandler.handle(outboxId, CLAIMED, PROCESSING);
        });
        if (!ok) {
            log.info("Outbox: {} failed to change status from {} to {}", outboxId, CLAIMED, PROCESSING);
            return;
        }
        var createDestinationStepResult = context.runStepOnce("destination", () -> {
            log.info("Creating destinations for outbox: {}", outboxId);
            return createDestinationStepHandler.handle(outboxId);
        });
        var fetchPayloadStepResult = context.runStepOnce("fetchPayload", () -> {
            log.info("Fetching payload for history: {}", historyId);
            return fetchPayloadStepHandler.handle(historyId);
        });
        var destinationEntities = createDestinationStepResult.result();
        var payload = fetchPayloadStepResult.result()
                .payload();
        for (var destinationEntity : destinationEntities) {
            context.runStepOnce("schedule:" + destinationEntity.getId(), () -> {
                final var destinationId = destinationEntity.getId();
                final var destination = destinationEntity.getDestination();
                log.info("Scheduling destinationId: {} to destination: {}", destinationId, destination);
                var _ = scheduleDestinationStepHandler.handle(destinationEntity.getId(),
                        payload,
                        destinationEntity.getDestination());
            });
        }
    }
}
