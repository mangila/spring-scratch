package com.github.mangila.app.director.outbox.destination.http;

import com.github.mangila.app.director.service.DirectorOutboxDestinationService;
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
public class DirectorHttpDestinationJobHandler implements JobRequestHandler<DirectorHttpDestinationJobRequest> {

	private static final Logger log = new JobRunrDashboardLogger(
			LoggerFactory.getLogger(DirectorHttpDestinationJobHandler.class));

	private final TransactionTemplate transactionTemplate;

	private final DirectorOutboxDestinationService destinationService;

	private final RestClient restClient;

	public DirectorHttpDestinationJobHandler(TransactionTemplate transactionTemplate,
			DirectorOutboxDestinationService directorOutboxDestinationService, RestClient.Builder restClient) {
		this.transactionTemplate = transactionTemplate;
		this.destinationService = directorOutboxDestinationService;
		this.restClient = restClient.baseUrl("https://httpbin.org")
			.requestFactory(new JdkClientHttpRequestFactory())
			.defaultHeader("Content-Type", "application/json")
			.defaultHeader("Accept", "application/json")
			.build();
	}

	@Override
	public void run(DirectorHttpDestinationJobRequest jobRequest) throws Exception {
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
