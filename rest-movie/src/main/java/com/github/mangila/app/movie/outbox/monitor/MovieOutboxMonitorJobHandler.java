package com.github.mangila.app.movie.outbox.monitor;

import com.github.mangila.app.movie.service.MovieOutboxDestinationService;
import com.github.mangila.app.movie.service.MovieOutboxService;
import com.github.mangila.app.movie.service.MovieOutboxVersionService;
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
public class MovieOutboxMonitorJobHandler implements JobRequestHandler<MovieOutboxMonitorJobRequest> {

    private static final Logger log = new JobRunrDashboardLogger(
            LoggerFactory.getLogger(MovieOutboxMonitorJobHandler.class));

    private final TransactionTemplate transactionTemplate;
    private final MovieOutboxVersionService movieOutboxVersionService;
    private final MovieOutboxService movieOutboxService;
    private final MovieOutboxDestinationService movieOutboxDestinationService;

    public MovieOutboxMonitorJobHandler(TransactionTemplate transactionTemplate,
                                        MovieOutboxVersionService movieOutboxVersionService,
                                        MovieOutboxService movieOutboxService,
                                        MovieOutboxDestinationService movieOutboxDestinationService) {
        this.transactionTemplate = transactionTemplate;
        this.movieOutboxVersionService = movieOutboxVersionService;
        this.movieOutboxService = movieOutboxService;
        this.movieOutboxDestinationService = movieOutboxDestinationService;
    }

    @Override
    public void run(MovieOutboxMonitorJobRequest jobRequest) throws Exception {
        final var context = ThreadLocalJobContext.getJobContext();
        final var limit = jobRequest.limit();

        var outboxes = movieOutboxService.findAllByStatus(Status.PROCESSING, limit);
        log.info("Found {} outboxes to monitor", outboxes.size());

        var errors = new ArrayList<UUID>(outboxes.size());
        for (var outbox : outboxes) {
            var outboxId = outbox.id();
            try {
                var destinationEntities = movieOutboxDestinationService.findAllByOutboxId(outboxId);
                if (destinationEntities.isEmpty()) {
                    log.info("No destinations for outbox: {}", outboxId);
                    continue;
                }
                log.info("Found {} destinations for outbox: {}", destinationEntities.size(), outboxId);
                var allSuccessMatch = destinationEntities.stream()
                        .allMatch(destinationEntity -> destinationEntity.status() == Status.SUCCESS);
                if (allSuccessMatch) {
                    final var fromStatus = Status.PROCESSING;
                    final var toStatus = Status.SUCCESS;
                    transactionTemplate.executeWithoutResult(_ -> {
                        final boolean ok = movieOutboxService.changeStatus(outboxId, fromStatus, toStatus);
                        if (!ok) {
                            throw new IllegalStateException("Outbox: %s failed to change status from %s to %s".formatted(outboxId, fromStatus, toStatus));
                        }
                        movieOutboxVersionService.increment(outbox.aggregateId());
                    });
                }
            } catch (Exception e) {
                log.error("Error while monitoring outbox: {} - {}", outboxId, e.getMessage(), e);
                errors.add(outboxId);
            }
        }

        if (CollectionUtils.isNotNullOrEmpty(errors)) {
            throw new IllegalStateException("Failed to monitor outboxes: %s".formatted(errors));
        }

    }
}
