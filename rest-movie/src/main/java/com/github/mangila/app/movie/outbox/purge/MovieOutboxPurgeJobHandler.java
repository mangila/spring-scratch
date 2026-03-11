package com.github.mangila.app.movie.outbox.purge;

import com.github.mangila.app.movie.properties.MovieProperties;
import com.github.mangila.app.movie.service.MovieOutboxDestinationService;
import com.github.mangila.app.movie.service.MovieOutboxService;
import com.github.mangila.app.shared.persistence.base.projection.OutboxDestinationProjection;
import com.github.mangila.app.shared.persistence.type.Status;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.server.runner.ThreadLocalJobContext;
import org.jobrunr.utils.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

@Component
public class MovieOutboxPurgeJobHandler implements JobRequestHandler<MovieOutboxPurgeJobRequest> {

    private static final Logger log = new JobRunrDashboardLogger(
            LoggerFactory.getLogger(MovieOutboxPurgeJobHandler.class));

    private final JsonMapper jsonMapper;
    private final TransactionTemplate transactionTemplate;
    private final MovieOutboxService movieOutboxService;
    private final MovieOutboxDestinationService movieOutboxDestinationService;

    public MovieOutboxPurgeJobHandler(JsonMapper jsonMapper,
                                      TransactionTemplate transactionTemplate,
                                      MovieOutboxService movieOutboxService,
                                      MovieOutboxDestinationService movieOutboxDestinationService) {
        this.jsonMapper = jsonMapper;
        this.transactionTemplate = transactionTemplate;
        this.movieOutboxService = movieOutboxService;
        this.movieOutboxDestinationService = movieOutboxDestinationService;
    }

    @Override
    public void run(MovieOutboxPurgeJobRequest jobRequest) throws Exception {
        final var context = ThreadLocalJobContext.getJobContext();
        final var limit = jobRequest.limit();

        String batchAsJson = context.runStepOnce("claim", () -> {
            var claim = transactionTemplate.execute(_ -> {
                return movieOutboxService.claimBatch(
                        Status.SUCCESS,
                        Status.DELETING,
                        limit);
            });
            Objects.requireNonNull(claim, "claim returned null");
            return jsonMapper.writeValueAsString(claim);
        });

        UUID[] batchIds = jsonMapper.readValue(batchAsJson, UUID[].class);

        if (CollectionUtils.isNullOrEmpty(batchIds)) {
            log.info("No outboxes to purge");
            return;
        }

        var outboxSuccess = new ArrayList<UUID>(batchIds.length);
        var destinationSuccess = new ArrayList<UUID>(batchIds.length * MovieProperties.SUPPORTED_DESTINATIONS.size());
        var errors = new ArrayList<UUID>(batchIds.length);
        for (var outboxId : batchIds) {
            try {
                String destinationIdsAsJson = context.runStepOnce("destination:%s".formatted(outboxId), () -> {
                    var destinationProjectionsIds = movieOutboxDestinationService.findAllByOutboxId(outboxId)
                            .stream()
                            .map(OutboxDestinationProjection::id)
                            .toList();
                    return jsonMapper.writeValueAsString(destinationProjectionsIds);
                });
                UUID[] destinationIds = jsonMapper.readValue(destinationIdsAsJson, UUID[].class);
                destinationSuccess.addAll(Arrays.asList(destinationIds));
                outboxSuccess.add(outboxId);
            } catch (Exception e) {
                errors.add(outboxId);
                log.error("Error while purging outbox: {} - {}", outboxId, e.getMessage(), e);
            }
        }

        log.info("Purging outboxes: {} with destinations: {} errors: {}", outboxSuccess.size(), destinationSuccess.size(), errors.size());

        if (CollectionUtils.isNotNullOrEmpty(outboxSuccess)) {
            transactionTemplate.executeWithoutResult(_ -> {
                movieOutboxService.deleteAllById(outboxSuccess);
                movieOutboxDestinationService.deleteAllById(destinationSuccess);
            });
        }

        if (CollectionUtils.isNotNullOrEmpty(errors)) {
            var errorString = String.join(",", errors.stream()
                    .map(UUID::toString)
                    .toList());
            context.saveMetadata("errors", errorString);
            throw new IllegalStateException("Failed to purge outboxes: %s".formatted(errors));
        }
    }
}
