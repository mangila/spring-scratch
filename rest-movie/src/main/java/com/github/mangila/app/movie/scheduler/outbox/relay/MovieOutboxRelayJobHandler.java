package com.github.mangila.app.movie.scheduler.outbox.relay;

import com.github.mangila.app.movie.properties.MovieProperties;
import com.github.mangila.app.movie.scheduler.outbox.relay.step.ScheduleOutboxProcessingStepHandler;
import com.github.mangila.app.movie.scheduler.outbox.shared.ClaimBatchStepHandler;
import com.github.mangila.app.shared.persistence.type.Status;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.server.runner.ThreadLocalJobContext;
import org.jobrunr.utils.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

@Component
public class MovieOutboxRelayJobHandler implements JobRequestHandler<MovieOutboxRelayJobRequest> {

    private static final Logger log = new JobRunrDashboardLogger(
            LoggerFactory.getLogger(MovieOutboxRelayJobHandler.class));

    private final MovieProperties movieProperties;
    private final JsonMapper jsonMapper;
    private final ClaimBatchStepHandler claimBatchStepHandler;
    private final ScheduleOutboxProcessingStepHandler scheduleOutboxprocessingStepHandler;

    public MovieOutboxRelayJobHandler(MovieProperties movieProperties,
                                      JsonMapper jsonMapper,
                                      ClaimBatchStepHandler claimBatchStepHandler,
                                      ScheduleOutboxProcessingStepHandler scheduleOutboxprocessingStepHandler) {
        this.movieProperties = movieProperties;
        this.jsonMapper = jsonMapper;
        this.claimBatchStepHandler = claimBatchStepHandler;
        this.scheduleOutboxprocessingStepHandler = scheduleOutboxprocessingStepHandler;
    }

    @Override
    public void run(MovieOutboxRelayJobRequest jobRequest) throws Exception {
        final var limit = jobRequest.limit();
        final var context = ThreadLocalJobContext.getJobContext();
        String jsonBatch = context.runStepOnce("batch", () -> {
            log.info("Claiming outbox jsonBatch with limit: {}", limit);
            return claimBatchStepHandler.handle(Status.PENDING, Status.CLAIMED, limit);
        });
        UUID[] batch = jsonMapper.readValue(jsonBatch, UUID[].class);
        if (CollectionUtils.isNullOrEmpty(batch)) {
            log.info("No outboxes to process");
            return;
        }
        log.info("Processing {} outboxes", batch.length);
        for (var outboxId : batch) {
            context.runStepOnce("schedule:" + outboxId, () -> {
                var jobId = scheduleOutboxprocessingStepHandler.handle(outboxId);
                log.info("Scheduling outbox for processing: {} - jobId: {}", outboxId, jobId.asUUID());
            });
        }
    }

}
