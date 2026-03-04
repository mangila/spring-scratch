package com.github.mangila.app.movie.scheduler.outbox.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.mangila.app.movie.properties.MovieProperties;
import com.github.mangila.app.movie.scheduler.MovieScheduler;
import com.github.mangila.app.movie.scheduler.outbox.consumer.destination.http.MovieHttpDestinationJobRequest;
import com.github.mangila.app.movie.scheduler.outbox.consumer.destination.kafka.MovieKafkaDestinationJobRequest;
import com.github.mangila.app.movie.scheduler.outbox.consumer.step.ClaimStep;
import com.github.mangila.app.movie.scheduler.outbox.consumer.step.PayloadStep;
import com.github.mangila.app.movie.service.MovieHistoryService;
import com.github.mangila.app.movie.service.MovieOutboxDestinationService;
import com.github.mangila.app.movie.service.MovieOutboxService;
import com.github.mangila.app.shared.persistence.type.Destination;
import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.server.runner.ThreadLocalJobContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

import static com.github.mangila.app.shared.persistence.type.Destination.HTTP;
import static com.github.mangila.app.shared.persistence.type.Destination.KAFKA;

@Component
public class MovieOutboxConsumeRelayJobHandler implements JobRequestHandler<MovieOutboxConsumeRelayJobRequest> {

	private static final Logger log = new JobRunrDashboardLogger(
			LoggerFactory.getLogger(MovieOutboxConsumeRelayJobHandler.class));

	private final MovieProperties movieProperties;

	private final TransactionTemplate transactionTemplate;

	private final MovieOutboxService movieOutboxService;

	private final MovieOutboxDestinationService movieOutboxDestinationService;

	private final MovieHistoryService movieHistoryService;

	private final Map<Destination, Function<ScheduleDestination, JobId>> destinationMap;

	public MovieOutboxConsumeRelayJobHandler(MovieProperties movieProperties, TransactionTemplate transactionTemplate,
			MovieOutboxService movieOutboxService, MovieOutboxDestinationService movieOutboxDestinationService,
			MovieScheduler movieScheduler, MovieHistoryService movieHistoryService) {
		this.movieProperties = movieProperties;
		this.transactionTemplate = transactionTemplate;
		this.movieOutboxService = movieOutboxService;
		this.movieOutboxDestinationService = movieOutboxDestinationService;
		this.movieHistoryService = movieHistoryService;
		this.destinationMap = Map.of(HTTP,
				obj -> movieScheduler
					.schedule(new MovieHttpDestinationJobRequest(obj.destinationId, obj.payload, obj.destination)),
				KAFKA, obj -> movieScheduler
					.schedule(new MovieKafkaDestinationJobRequest(obj.destinationId, obj.payload, obj.destination)));
	}

	@Override
	public void run(MovieOutboxConsumeRelayJobRequest jobRequest) throws Exception {
		final var context = ThreadLocalJobContext.getJobContext();
		final var limit = jobRequest.limit();
		ClaimStep claimStep = context.runStepOnce("claimDestinationPending", () -> {
			var l = transactionTemplate.execute(_ -> movieOutboxDestinationService.claimDestinationPending(limit));
			Objects.requireNonNull(l, "claimDestinationPending returned null");
			return new ClaimStep(l);
		});
		var destinationProjections = claimStep.destinationProjections();
		log.info("Movie outbox destination size: {}", destinationProjections.size());
		for (var destinationEntity : destinationProjections) {
			final var destinationId = destinationEntity.id();
			final var destinationIdAsString = destinationId.toString();
			PayloadStep payloadStep = context.runStepOnce("payload:" + destinationIdAsString, () -> {
				final var outboxId = destinationEntity.outboxId();
				final var outbox = movieOutboxService.findOutboxById(outboxId);
				final var historyId = outbox.historyId();
				final var projection = movieHistoryService.findPayloadById(historyId);
				log.info("Finished payload step for destination id: {}", destinationId);
				return new PayloadStep(projection);
			});
			var payload = payloadStep.projection().payload();
			context.runStepOnce("schedule:" + destinationIdAsString, () -> {
				final var destination = destinationEntity.destination();
				final var fn = destinationMap.get(destination);
				if (fn == null) {
					throw new IllegalStateException("Unknown destination: " + destination);
				}
				var jobId = fn.apply(new ScheduleDestination(destinationId, payload, destination));
				log.info("Finished schedule step for destination id: {} - {} - jobId: {}", destinationId, destination,
						jobId.asUUID());
			});
		}
	}

	private record ScheduleDestination(UUID destinationId, JsonNode payload, Destination destination) {

	}

}
