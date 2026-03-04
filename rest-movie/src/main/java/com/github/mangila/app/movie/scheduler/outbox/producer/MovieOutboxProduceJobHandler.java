package com.github.mangila.app.movie.scheduler.outbox.producer;

import com.github.mangila.app.movie.persistance.outbox.destination.MovieOutboxDestinationEntity;
import com.github.mangila.app.movie.scheduler.MovieScheduler;
import com.github.mangila.app.movie.service.MovieOutboxService;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.server.runner.ThreadLocalJobContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;

@Component
public class MovieOutboxProduceJobHandler implements JobRequestHandler<MovieOutboxProduceJobRequest> {

    private static final Logger log = new JobRunrDashboardLogger(
            LoggerFactory.getLogger(MovieOutboxProduceJobHandler.class));

    private final TransactionTemplate transactionTemplate;

    private final MovieOutboxService movieOutboxService;

    private final MovieScheduler movieScheduler;

    public MovieOutboxProduceJobHandler(TransactionTemplate transactionTemplate, MovieOutboxService movieOutboxService,
                                        MovieScheduler movieScheduler) {
        this.transactionTemplate = transactionTemplate;
        this.movieOutboxService = movieOutboxService;
        this.movieScheduler = movieScheduler;
    }

    @Override
    public void run(MovieOutboxProduceJobRequest jobRequest) throws Exception {
        final var context = ThreadLocalJobContext.getJobContext();
        final var outbox = jobRequest.outbox();
        transactionTemplate.executeWithoutResult(_ -> {
            var version = movieOutboxService.findVersionByIdWithXLock(outbox.aggregateId());
            if (Objects.equals(version.currentVersion(), outbox.aggregateVersion())) {
                log.info("version: {}", version);
                var entities = movieOutboxService.createDestinations(outbox.id());
                var destinations = entities.stream()
                        .map(MovieOutboxDestinationEntity::getDestination)
                        .toList();
                log.info("created destinations: {} - {}", outbox.id(), destinations);
            } else {
                log.warn("version mismatch: {} != {}", version.currentVersion(), outbox.aggregateVersion());
            }
        });
    }

}
