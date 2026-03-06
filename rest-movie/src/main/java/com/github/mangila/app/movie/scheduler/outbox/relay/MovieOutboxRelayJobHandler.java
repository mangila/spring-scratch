package com.github.mangila.app.movie.scheduler.outbox.relay;

import com.github.mangila.app.movie.scheduler.outbox.relay.step.ScheduleOutboxProcessingStep;
import com.github.mangila.app.movie.scheduler.outbox.shared.ClaimOutboxBatchStepHandler;
import com.github.mangila.app.shared.persistence.type.Status;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.server.runner.ThreadLocalJobContext;
import org.jobrunr.utils.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.UUID;

@Component
public class MovieOutboxRelayJobHandler implements JobRequestHandler<MovieOutboxRelayJobRequest> {

    private static final Logger log = new JobRunrDashboardLogger(
            LoggerFactory.getLogger(MovieOutboxRelayJobHandler.class));

    private final JsonMapper jsonMapper;
    private final ClaimOutboxBatchStepHandler claimOutboxBatchStepHandler;
    private final ScheduleOutboxProcessingStep scheduleOutboxprocessingStep;

    public MovieOutboxRelayJobHandler(JsonMapper jsonMapper,
                                      ClaimOutboxBatchStepHandler claimOutboxBatchStepHandler,
                                      ScheduleOutboxProcessingStep scheduleOutboxprocessingStep) {
        this.jsonMapper = jsonMapper;
        this.claimOutboxBatchStepHandler = claimOutboxBatchStepHandler;
        this.scheduleOutboxprocessingStep = scheduleOutboxprocessingStep;
    }

    @Override
    public void run(MovieOutboxRelayJobRequest jobRequest) throws Exception {
        final var limit = jobRequest.limit();
        final var context = ThreadLocalJobContext.getJobContext();

        /*
         * Returns a String/JSON representation
         * JobRunr behaves better with primitive values than custom objects in the metadata object
         */
        final String jsonBatch = context.runStepOnce("batch", () -> {
            log.info("Claiming outbox batch with limit: {}", limit);
            return claimOutboxBatchStepHandler.handle(Status.PENDING, Status.CLAIMED, limit);
        });

        final UUID[] batch = jsonMapper.readValue(jsonBatch, UUID[].class);

        if (CollectionUtils.isNullOrEmpty(batch)) {
            log.info("No outboxes to process");
            return;
        }

        log.info("Processing {} outboxes", batch.length);
        final var errors = new ArrayList<UUID>(batch.length);
        for (var outboxId : batch) {
            try {
                context.runStepOnce("schedule:" + outboxId, () -> {
                    var jobId = scheduleOutboxprocessingStep.execute(outboxId);
                    log.info("Scheduled outbox processing for outbox: {} - jobId - {}", outboxId, jobId);
                });
            } catch (Exception e) {
                errors.add(outboxId);
                log.error("Error processing outbox: {} - {}", outboxId, e.getMessage(), e);
            }
        }
        if (CollectionUtils.isNotNullOrEmpty(errors)) {
            throw new IllegalStateException("Failed to process outboxes: %s".formatted(errors));
        }
    }

}
