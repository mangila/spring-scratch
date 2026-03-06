package com.github.mangila.app.movie.scheduler.outbox.purge;

import com.github.mangila.app.movie.properties.MovieProperties;
import com.github.mangila.app.movie.service.MovieOutboxDestinationService;
import com.github.mangila.app.movie.service.MovieOutboxService;
import com.github.mangila.app.shared.persistence.base.projection.OutboxDestinationProjection;
import com.github.mangila.app.shared.persistence.base.projection.OutboxProjection;
import com.github.mangila.app.shared.persistence.type.Status;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.server.runner.ThreadLocalJobContext;
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

    private final TransactionTemplate transactionTemplate;
    private final MovieOutboxService movieOutboxService;
    private final MovieOutboxDestinationService movieOutboxDestinationService;

    public MovieOutboxPurgeJobHandler(TransactionTemplate transactionTemplate,
                                      MovieOutboxService movieOutboxService,
                                      MovieOutboxDestinationService movieOutboxDestinationService) {
        this.transactionTemplate = transactionTemplate;
        this.movieOutboxService = movieOutboxService;
        this.movieOutboxDestinationService = movieOutboxDestinationService;
    }

    @Override
    public void run(MovieOutboxPurgeJobRequest jobRequest) throws Exception {
        final var context = ThreadLocalJobContext.getJobContext();
        final var limit = jobRequest.limit();

        var outboxIds = movieOutboxService.findAllByStatus(Status.SUCCESS, limit)
                .stream()
                .map(OutboxProjection::id)
                .toList();
        var destinationsIds = new ArrayList<UUID>(outboxIds.size() * MovieProperties.SUPPORTED_DESTINATIONS.size());
        for (var outboxId : outboxIds) {
            try {
                var destinationProjectionsIds = movieOutboxDestinationService.findAllByOutboxId(outboxId)
                        .stream()
                        .map(OutboxDestinationProjection::id)
                        .toList();
                destinationsIds.addAll(destinationProjectionsIds);
            } catch (Exception e) {
                log.error("Error while purging outbox: {} - {}", outboxId, e.getMessage(), e);
            }
        }

        transactionTemplate.executeWithoutResult(_ -> {
            movieOutboxService.deleteAllById(outboxIds);
            movieOutboxDestinationService.deleteAllById(destinationsIds);
        });

        log.info("Purged outbox: {} and destinations: {}", outboxIds.size(), destinationsIds.size());
    }
}
