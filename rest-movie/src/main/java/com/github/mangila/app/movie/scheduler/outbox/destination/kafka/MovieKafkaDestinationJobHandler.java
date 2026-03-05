package com.github.mangila.app.movie.scheduler.outbox.destination.kafka;

import com.github.mangila.app.movie.service.MovieOutboxDestinationService;
import com.github.mangila.app.shared.persistence.type.Status;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.server.runner.ThreadLocalJobContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class MovieKafkaDestinationJobHandler implements JobRequestHandler<MovieKafkaDestinationJobRequest> {

	private static final Logger log = new JobRunrDashboardLogger(
			LoggerFactory.getLogger(MovieKafkaDestinationJobHandler.class));

	private final TransactionTemplate transactionTemplate;

	private final MovieOutboxDestinationService destinationService;

	public MovieKafkaDestinationJobHandler(TransactionTemplate transactionTemplate,
			MovieOutboxDestinationService movieOutboxDestinationService) {
		this.transactionTemplate = transactionTemplate;
		this.destinationService = movieOutboxDestinationService;
	}

	@Override
	public void run(MovieKafkaDestinationJobRequest jobRequest) throws Exception {
		final var context = ThreadLocalJobContext.getJobContext();
		final var destinationId = jobRequest.destinationId();
		final var payload = jobRequest.payload();
		final var destination = jobRequest.destination();
		context.runStepOnce("destination", () -> {
			// simulate Kafka destination
			Thread.sleep(1000);
		});
		transactionTemplate.executeWithoutResult(_ -> {
			destinationService.updateDestinationStatus(destinationId, Status.SUCCESS);
		});
		log.info("Destination {} - {} - success", destinationId, destination);
	}

}
