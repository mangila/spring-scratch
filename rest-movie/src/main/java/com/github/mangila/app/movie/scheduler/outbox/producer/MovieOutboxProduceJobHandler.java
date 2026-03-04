package com.github.mangila.app.movie.scheduler.outbox.producer;

import com.github.mangila.app.movie.persistance.outbox.destination.MovieOutboxDestinationEntity;
import com.github.mangila.app.movie.service.MovieOutboxDestinationService;
import com.github.mangila.app.movie.service.MovieOutboxVersionService;
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

	private final MovieOutboxVersionService versionService;

	private final MovieOutboxDestinationService destinationService;

	public MovieOutboxProduceJobHandler(TransactionTemplate transactionTemplate,
			MovieOutboxVersionService versionService, MovieOutboxDestinationService destinationService) {
		this.transactionTemplate = transactionTemplate;
		this.versionService = versionService;
		this.destinationService = destinationService;
	}

	@Override
	public void run(MovieOutboxProduceJobRequest jobRequest) throws Exception {
		final var context = ThreadLocalJobContext.getJobContext();
		final var outbox = jobRequest.outbox();

		var ok = context.runStepOnce("outbox", () -> {
			return transactionTemplate.execute(_ -> {
				var version = versionService.findVersionByIdWithXLock(outbox.aggregateId());
				if (Objects.equals(version.currentVersion(), outbox.aggregateVersion())) {
					return true;
				}
				else {
					throw new RuntimeException(
							"Version mismatch: " + version.currentVersion() + " != " + outbox.aggregateVersion());
				}
			});
		});

		if (ok) {
			var entities = destinationService.createDestinations(outbox.id());
			var destinations = entities.stream().map(MovieOutboxDestinationEntity::getDestination).toList();
			log.info("Created destinations: {} - {}", outbox.id(), destinations);
		}
	}

}
