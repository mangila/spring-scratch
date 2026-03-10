package com.github.mangila.app.movie.outbox.destination.http;

import com.github.mangila.app.movie.service.MovieOutboxDestinationService;
import com.github.mangila.app.shared.persistence.type.Status;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.server.runner.ThreadLocalJobContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

@Component
public class MovieHttpDestinationJobHandler implements JobRequestHandler<MovieHttpDestinationJobRequest> {

    private static final Logger log = new JobRunrDashboardLogger(
            LoggerFactory.getLogger(MovieHttpDestinationJobHandler.class));

    private final TransactionTemplate transactionTemplate;

    private final MovieOutboxDestinationService destinationService;

    private final RestClient restClient;

    public MovieHttpDestinationJobHandler(TransactionTemplate transactionTemplate,
                                          MovieOutboxDestinationService movieOutboxDestinationService, RestClient.Builder restClient) {
        this.transactionTemplate = transactionTemplate;
        this.destinationService = movieOutboxDestinationService;
        this.restClient = restClient.baseUrl("https://httpbin.org")
                .requestFactory(new JdkClientHttpRequestFactory())
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @Override
    public void run(MovieHttpDestinationJobRequest jobRequest) throws Exception {
        final var context = ThreadLocalJobContext.getJobContext();
        final var destinationId = jobRequest.destinationId();
        final var destination = jobRequest.destination();
        transactionTemplate.executeWithoutResult(_ -> {
            final var fromStatus = Status.CLAIMED;
            final var toStatus = Status.PROCESSING;
            destinationService.updateStatus(destinationId, fromStatus, toStatus);
        });
        log.info("Sending movie to destination: {}", destinationId);
        transactionTemplate.executeWithoutResult(_ -> {
            final var fromStatus = Status.PROCESSING;
            final var toStatus = Status.SUCCESS;
            destinationService.updateStatus(destinationId, fromStatus, toStatus);
        });
    }

}
