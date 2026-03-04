package com.github.mangila.app.movie.scheduler.outbox.producer;

import com.github.mangila.app.movie.scheduler.MovieScheduler;
import com.github.mangila.app.movie.scheduler.outbox.producer.step.ClaimStep;
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
public class MovieOutboxProduceRelayJobHandler implements JobRequestHandler<MovieOutboxProduceRelayJobRequest> {

	private static final Logger log = new JobRunrDashboardLogger(
			LoggerFactory.getLogger(MovieOutboxProduceRelayJobHandler.class));

	private final TransactionTemplate transactionTemplate;

	private final MovieOutboxService outboxService;

	private final MovieScheduler movieScheduler;

	public MovieOutboxProduceRelayJobHandler(TransactionTemplate transactionTemplate,
			MovieOutboxService movieOutboxService, MovieScheduler movieScheduler) {
		this.transactionTemplate = transactionTemplate;
		this.outboxService = movieOutboxService;
		this.movieScheduler = movieScheduler;
	}

	@Override
	public void run(MovieOutboxProduceRelayJobRequest jobRequest) throws Exception {
		final var context = ThreadLocalJobContext.getJobContext();
		final var limit = jobRequest.limit();
		ClaimStep claimStep = context.runStepOnce("claim", () -> {
			var l = transactionTemplate.execute(_ -> outboxService.claimOutboxPending(limit));
			Objects.requireNonNull(l, "claimOutboxPending returned null");
			return new ClaimStep(l);
		});
		var outboxProjections = claimStep.outboxProjections();
		log.info("Movie outbox relay size: {}", outboxProjections.size());
		for (var outbox : outboxProjections) {
			final var outboxIdAsString = outbox.id().toString();
			context.runStepOnce(outboxIdAsString, () -> {
				var jobId = movieScheduler.schedule(new MovieOutboxProduceJobRequest(outbox));
				log.info("Scheduled outbox: {} - {} - jobId: {}", outboxIdAsString, outbox, jobId.asUUID());
			});
		}
	}

}
