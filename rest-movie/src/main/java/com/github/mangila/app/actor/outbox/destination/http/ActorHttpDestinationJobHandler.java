package com.github.mangila.app.actor.outbox.destination.http;

import com.github.mangila.app.actor.service.ActorOutboxDestinationService;
import com.github.mangila.app.shared.persistence.type.Status;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.server.runner.ThreadLocalJobContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

@Component
public class ActorHttpDestinationJobHandler implements JobRequestHandler<ActorHttpDestinationJobRequest> {

	private static final Logger log = new JobRunrDashboardLogger(
			LoggerFactory.getLogger(ActorHttpDestinationJobHandler.class));

	private final TransactionTemplate transactionTemplate;

	private final ActorOutboxDestinationService destinationService;

	private final RestClient restClient;

	public ActorHttpDestinationJobHandler(TransactionTemplate transactionTemplate,
			ActorOutboxDestinationService destinationService, RestClient.Builder restClient) {
		this.transactionTemplate = transactionTemplate;
		this.destinationService = destinationService;
		this.restClient = restClient.baseUrl("https://httpbin.org")
			.requestFactory(new JdkClientHttpRequestFactory())
			.defaultHeader("Content-Type", "application/json")
			.defaultHeader("Accept", "application/json")
			.build();
	}

	@Override
	public void run(ActorHttpDestinationJobRequest jobRequest) throws Exception {
		final var context = ThreadLocalJobContext.getJobContext();
		final var destinationId = jobRequest.destinationId();
		final var destination = jobRequest.destination();

		context.runStepOnce("process", () -> {
			final var fromStatus = Status.CLAIMED;
			final var toStatus = Status.PROCESSING;
			transactionTemplate.executeWithoutResult(_ -> {
				destinationService.updateStatus(destinationId, fromStatus, toStatus);
			});
			log.info("Changed status from: {} to: {}", fromStatus, toStatus);
		});

		context.runStepOnce("send", () -> {
			restClient.post()
				.uri("/post")
				.body("asdf")
				.retrieve()
				.onStatus(HttpStatusCode::isError, (request, response) -> {
					throw new RuntimeException("Failed to send message");
				});
			log.info("Sent message to: {}", destination);
		});

		context.runStepOnce("success", () -> {
			final var fromStatus = Status.PROCESSING;
			final var toStatus = Status.SUCCESS;
			transactionTemplate.executeWithoutResult(_ -> {
				destinationService.updateStatus(destinationId, fromStatus, toStatus);
			});
			log.info("Changed status from: {} to: {}", fromStatus, toStatus);
		});
	}

}
