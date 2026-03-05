package com.github.mangila.app.movie.scheduler.outbox.destination;

import com.github.mangila.app.movie.service.MovieOutboxDestinationService;
import com.github.mangila.app.movie.service.MovieOutboxService;
import com.github.mangila.app.shared.persistence.type.Status;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.utils.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.UUID;

@Component
public class MovieOutboxDestinationMonitorJobHandler
		implements JobRequestHandler<MovieOutboxDestinationMonitorJobRequest> {

	private static final Logger log = new JobRunrDashboardLogger(
			LoggerFactory.getLogger(MovieOutboxDestinationMonitorJobHandler.class));

	private final MovieOutboxService outboxService;

	private final MovieOutboxDestinationService destinationService;

	private final TransactionTemplate transactionTemplate;

	public MovieOutboxDestinationMonitorJobHandler(MovieOutboxService movieOutboxService,
			MovieOutboxDestinationService movieOutboxDestinationService, TransactionTemplate transactionTemplate) {
		this.outboxService = movieOutboxService;
		this.destinationService = movieOutboxDestinationService;
		this.transactionTemplate = transactionTemplate;
	}

	@Override
	public void run(MovieOutboxDestinationMonitorJobRequest jobRequest) throws Exception {
		var completedOutboxIds = transactionTemplate.execute(_ -> {
			var completedDestinations = new ArrayList<UUID>(256);
			try (var stream = outboxService.streamOutboxIds(500)) {
				stream.forEach(id -> {
					var projections = destinationService.findAllByOutboxId(id);
					if (CollectionUtils.isNotNullOrEmpty(projections)) {
						log.info("Found {} destination projections for outbox id: {}", projections.size(), id);
						var ok = projections.stream().allMatch(projection -> projection.status() == Status.SUCCESS);
						if (ok) {
							completedDestinations.add(id);
						}
					}
				});
			}
			return completedDestinations;
		});

		transactionTemplate.executeWithoutResult(_ -> {
			log.info("Marking {} outbox as success", completedOutboxIds.size());
			outboxService.bulkChangeStatus(completedOutboxIds, Status.SUCCESS);
		});
	}

}
