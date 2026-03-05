package com.github.mangila.app.movie.scheduler.outbox.monitor;

import com.github.mangila.app.movie.service.MovieOutboxDestinationService;
import com.github.mangila.app.movie.service.MovieOutboxService;
import com.github.mangila.app.movie.service.MovieOutboxVersionService;
import com.github.mangila.app.shared.persistence.type.Status;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.utils.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class MovieOutboxMonitorJobHandler implements JobRequestHandler<MovieOutboxMonitorJobRequest> {

    private static final Logger log = new JobRunrDashboardLogger(
            LoggerFactory.getLogger(MovieOutboxMonitorJobHandler.class));

    private final TransactionTemplate transactionTemplate;
    private final MovieOutboxService movieOutboxService;
    private final MovieOutboxDestinationService movieOutboxDestinationService;
    private final MovieOutboxVersionService movieOutboxVersionService;

    public MovieOutboxMonitorJobHandler(TransactionTemplate transactionTemplate,
                                        MovieOutboxService movieOutboxService,
                                        MovieOutboxDestinationService movieOutboxDestinationService,
                                        MovieOutboxVersionService movieOutboxVersionService) {
        this.transactionTemplate = transactionTemplate;
        this.movieOutboxService = movieOutboxService;
        this.movieOutboxDestinationService = movieOutboxDestinationService;
        this.movieOutboxVersionService = movieOutboxVersionService;
    }

    @Override
    public void run(MovieOutboxMonitorJobRequest jobRequest) throws Exception {
        final var limit = jobRequest.limit();
        var projections = movieOutboxService.findAllByStatus(Status.PROCESSING, limit);
        if (CollectionUtils.isNullOrEmpty(projections)) {
            log.info("No outbox to check");
            return;
        }
        for (var outbox : projections) {
            final var outboxId = outbox.id();
            final var aggregateId = outbox.aggregateId();
            log.info("Checking outbox: {}", outboxId);
            try {
                var destinationEntities = movieOutboxDestinationService.findAllByOutboxId(outboxId);
                var allMatch = destinationEntities
                        .stream()
                        .allMatch(destination -> destination.getStatus() == Status.SUCCESS);
                if (CollectionUtils.isNotNullOrEmpty(destinationEntities) && allMatch) {
                    transactionTemplate.executeWithoutResult(_ -> {
                        movieOutboxService.changeStatus(outboxId, Status.PROCESSING, Status.SUCCESS);
                        movieOutboxVersionService.increment(aggregateId);
                        log.info("Success: {}", outboxId);
                    });
                }
            } catch (Exception e) {
                log.error("Error while checking outbox: {} - {}", outboxId, e.getMessage(), e);
            }
        }
    }
}
