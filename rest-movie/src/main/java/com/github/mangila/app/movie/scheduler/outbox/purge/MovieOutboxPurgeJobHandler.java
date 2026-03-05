package com.github.mangila.app.movie.scheduler.outbox.purge;

import com.github.mangila.app.movie.properties.MovieProperties;
import com.github.mangila.app.movie.scheduler.outbox.purge.step.FindOutboxDestinationStepHandler;
import com.github.mangila.app.movie.scheduler.outbox.shared.ClaimBatchStepHandler;
import com.github.mangila.app.movie.service.MovieOutboxDestinationService;
import com.github.mangila.app.movie.service.MovieOutboxService;
import com.github.mangila.app.shared.persistence.base.OutboxDestinationBaseEntity;
import com.github.mangila.app.shared.persistence.base.projection.OutboxProjection;
import com.github.mangila.app.shared.persistence.type.Status;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.server.runner.ThreadLocalJobContext;
import org.jobrunr.utils.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.UUID;

@Component
public class MovieOutboxPurgeJobHandler implements JobRequestHandler<MovieOutboxPurgeJobRequest> {

    private static final Logger log = new JobRunrDashboardLogger(
            LoggerFactory.getLogger(MovieOutboxPurgeJobHandler.class));

    private final MovieProperties movieProperties;
    private final TransactionTemplate transactionTemplate;
    private final MovieOutboxDestinationService movieOutboxDestinationService;
    private final MovieOutboxService movieOutboxService;
    private final ClaimBatchStepHandler claimBatchStepHandler;
    private final FindOutboxDestinationStepHandler findOutboxDestinationStepHandler;

    public MovieOutboxPurgeJobHandler(MovieProperties movieProperties,
                                      TransactionTemplate transactionTemplate,
                                      MovieOutboxDestinationService movieOutboxDestinationService,
                                      MovieOutboxService movieOutboxService,
                                      ClaimBatchStepHandler claimBatchStepHandler,
                                      FindOutboxDestinationStepHandler findOutboxDestinationStepHandler) {
        this.movieProperties = movieProperties;
        this.transactionTemplate = transactionTemplate;
        this.movieOutboxDestinationService = movieOutboxDestinationService;
        this.movieOutboxService = movieOutboxService;
        this.claimBatchStepHandler = claimBatchStepHandler;
        this.findOutboxDestinationStepHandler = findOutboxDestinationStepHandler;
    }

    @Override
    public void run(MovieOutboxPurgeJobRequest jobRequest) throws Exception {
        final var context = ThreadLocalJobContext.getJobContext();
        final var destinationPropertiesSize = movieProperties.getOutbox()
                .getDestinations()
                .size();
        final var limit = jobRequest.limit();
        var batch = context.runStepOnce("claimBatch", () -> {
            log.info("Claiming outbox batch with limit: {}", limit);
            return claimBatchStepHandler.handle(Status.SUCCESS, Status.DELETING, limit);
        });
        var batchResult = batch.result();
        if (CollectionUtils.isNullOrEmpty(batchResult)) {
            log.info("No outboxes to delete");
            return;
        }
        var outboxIds = batchResult.stream()
                .map(OutboxProjection::id)
                .toList();
        var destinationsIds = new ArrayList<UUID>(outboxIds.size() * destinationPropertiesSize);
        for (var outboxId : outboxIds) {
            var findOutboxDestinationStepResult = context.runStepOnce("findOutboxDestination:" + outboxId, () -> {
                log.info("Run step findOutboxDestination - {}", outboxId);
                return findOutboxDestinationStepHandler.handle(outboxId, Status.SUCCESS);
            });
            var destinations = findOutboxDestinationStepResult.result();
            destinationsIds.addAll(destinations.stream()
                    .map(OutboxDestinationBaseEntity::getId)
                    .toList());
        }

        context.runStepOnce("deleteOutbox", () -> {
            log.info("Run step deleteOutbox");
            transactionTemplate.executeWithoutResult(_ -> {
                movieOutboxService.deleteAllById(outboxIds);
                if (CollectionUtils.isNotNullOrEmpty(destinationsIds)) {
                    movieOutboxDestinationService.deleteAllById(destinationsIds);
                } else {
                    log.info("No destinations to delete");
                }
            });
        });
        log.info("Deleted {} outboxes and {} destinations", outboxIds.size(), destinationsIds.size());
    }

}
