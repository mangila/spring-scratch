package com.github.mangila.app.movie.scheduler.outbox.consumer;

import com.github.mangila.app.movie.properties.MovieProperties;
import com.github.mangila.app.movie.scheduler.MovieScheduler;
import com.github.mangila.app.movie.scheduler.outbox.consumer.destination.http.MovieHttpDestinationJobRequest;
import com.github.mangila.app.movie.scheduler.outbox.consumer.destination.kafka.MovieKafkaDestinationJobRequest;
import com.github.mangila.app.movie.service.MovieHistoryService;
import com.github.mangila.app.movie.service.MovieOutboxService;
import com.github.mangila.app.shared.persistence.base.projection.HistoryPayloadProjection;
import com.github.mangila.app.shared.persistence.base.projection.OutboxDestinationProjection;
import com.github.mangila.app.shared.persistence.type.Destination;
import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.server.runner.ThreadLocalJobContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import static com.github.mangila.app.shared.persistence.type.Destination.*;

@Component
public class MovieOutboxConsumeRelayJobHandler implements JobRequestHandler<MovieOutboxConsumeRelayJobRequest> {

	private static final Logger log = new JobRunrDashboardLogger(
			LoggerFactory.getLogger(MovieOutboxConsumeRelayJobHandler.class));

	private final MovieProperties movieProperties;

	private final TransactionTemplate transactionTemplate;

	private final MovieOutboxService movieOutboxService;

	private final MovieScheduler movieScheduler;

	private final MovieHistoryService movieHistoryService;

	private final Map<Destination, Function<HistoryPayloadProjection, JobId>> destinationMap;

	public MovieOutboxConsumeRelayJobHandler(MovieProperties movieProperties, TransactionTemplate transactionTemplate,
			MovieOutboxService movieOutboxService, MovieScheduler movieScheduler,
			MovieHistoryService movieHistoryService) {
		this.movieProperties = movieProperties;
		this.transactionTemplate = transactionTemplate;
		this.movieOutboxService = movieOutboxService;
		this.movieScheduler = movieScheduler;
		this.movieHistoryService = movieHistoryService;
		this.destinationMap = Map.of(HTTP,
				projection -> movieScheduler.schedule(new MovieHttpDestinationJobRequest(projection.payload())), KAFKA,
				projection -> movieScheduler.schedule(new MovieKafkaDestinationJobRequest(projection.payload())),
				RABBITMQ,
				projection -> movieScheduler.schedule(new MovieHttpDestinationJobRequest(projection.payload())), SQS,
				projection -> movieScheduler.schedule(new MovieHttpDestinationJobRequest(projection.payload())));
	}

	@Override
	public void run(MovieOutboxConsumeRelayJobRequest jobRequest) throws Exception {
		final var context = ThreadLocalJobContext.getJobContext();
		final var limit = jobRequest.limit();
		ClaimStep claimStep = context.runStepOnce("claimDestinationPending", () -> {
			var l = transactionTemplate.execute(_ -> movieOutboxService.claimDestinationPending(limit));
			Objects.requireNonNull(l, "claimDestinationPending returned null");
			return new ClaimStep(l);
		});
		var destinationProjections = claimStep.destinationProjections();
		log.info("Movie outbox destination size: {}", destinationProjections.size());
		for (var destinationEntity : destinationProjections) {
			final var idAsString = destinationEntity.id().toString();
			PayloadStep payloadStep = context.runStepOnce("payload:" + idAsString, () -> {
				final var outboxId = destinationEntity.outboxId();
				final var outbox = movieOutboxService.findOutboxById(outboxId);
				final var historyId = outbox.historyId();
				final var projection = movieHistoryService.findPayloadById(historyId);
				return new PayloadStep(projection);
			});
			var payload = payloadStep.payload();
			context.runStepOnce("schedule:" + idAsString, () -> {
				final var destination = destinationEntity.destination();
				final var jobId = destinationMap.get(destination).apply(payload);
				log.info("Scheduled job: {} - {}", jobId.asUUID(), destination);
			});
		}
	}

	private record ClaimStep(
			List<OutboxDestinationProjection> destinationProjections) implements JobContext.StepResult {
	}

	private record PayloadStep(HistoryPayloadProjection payload) implements JobContext.StepResult {
	}

}
