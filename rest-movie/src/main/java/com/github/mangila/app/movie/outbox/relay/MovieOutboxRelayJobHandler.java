package com.github.mangila.app.movie.outbox.relay;

import com.github.mangila.app.movie.outbox.relay.step.MovieOutboxRelayClaimStep;
import com.github.mangila.app.movie.outbox.relay.step.MovieOutboxRelayScheduleStep;
import com.github.mangila.app.movie.outbox.relay.step.MovieOutboxRelayStatusStep;
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

    private final MovieOutboxRelayClaimStep movieOutboxRelayClaimStep;
    private final MovieOutboxRelayScheduleStep movieOutboxRelayScheduleStep;
    private final MovieOutboxRelayStatusStep movieOutboxRelayStatusStep;
    private final JsonMapper jsonMapper;

    public MovieOutboxRelayJobHandler(MovieOutboxRelayClaimStep movieOutboxRelayClaimStep, MovieOutboxRelayScheduleStep movieOutboxRelayScheduleStep, MovieOutboxRelayStatusStep movieOutboxRelayStatusStep, JsonMapper jsonMapper) {
        this.movieOutboxRelayClaimStep = movieOutboxRelayClaimStep;
        this.movieOutboxRelayScheduleStep = movieOutboxRelayScheduleStep;
        this.movieOutboxRelayStatusStep = movieOutboxRelayStatusStep;
        this.jsonMapper = jsonMapper;
    }


    @Override
    public void run(MovieOutboxRelayJobRequest jobRequest) throws Exception {
        final var context = ThreadLocalJobContext.getJobContext();
        final var limit = jobRequest.limit();

        /*
         * Returns a String/JSON representation
         * JobRunr behaves better with primitive values than custom objects in the metadata object
         */
        final String jsonClaimed = context.runStepOnce("claim", () -> {
            final var fromStatus = Status.PENDING;
            final var toStatus = Status.CLAIMED;
            log.info("Claiming outbox batch with limit: {} - {} - {}", limit, fromStatus, toStatus);
            return movieOutboxRelayClaimStep.execute(fromStatus, toStatus, limit);
        });

        final UUID[] batch = jsonMapper.readValue(jsonClaimed, UUID[].class);

        if (CollectionUtils.isNullOrEmpty(batch)) {
            log.info("No outboxes to process");
            return;
        }

        log.info("Processing {} outboxes", batch.length);
        final var errors = new ArrayList<UUID>(batch.length);
        for (var outboxId : batch) {
            try {
                context.runStepOnce("schedule:%s".formatted(outboxId), () -> {
                    var jobId = movieOutboxRelayScheduleStep.execute(outboxId);
                    log.info("Scheduled outbox processing for outbox: {} - jobId - {}", outboxId, jobId);
                });
                context.runStepOnce("status:%s".formatted(outboxId), () -> {
                    final var fromStatus = Status.CLAIMED;
                    final var toStatus = Status.SCHEDULED;
                    final boolean execute = movieOutboxRelayStatusStep.execute(outboxId, fromStatus, toStatus);
                    if (!execute) {
                        throw new IllegalStateException("Outbox: %s failed to change status from %s to %s".formatted(outboxId, fromStatus, toStatus));
                    }
                    log.info("Changed status of outbox: {} from {} to {}", outboxId, fromStatus, toStatus);
                });
            } catch (Exception e) {
                errors.add(outboxId);
                log.error("Error processing outbox: {} - {}", outboxId, e.getMessage(), e);
            }
        }

        if (CollectionUtils.isNotNullOrEmpty(errors)) {
            var errorString = String.join(",", errors.stream()
                    .map(UUID::toString)
                    .toList());
            context.saveMetadata("errors", errorString);
            throw new IllegalStateException("Failed to process outboxes: %s".formatted(errorString));
        }
    }

}
