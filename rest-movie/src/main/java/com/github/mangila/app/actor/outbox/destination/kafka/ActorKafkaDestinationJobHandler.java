package com.github.mangila.app.actor.outbox.destination.kafka;

import com.github.mangila.app.actor.service.ActorOutboxDestinationService;
import com.github.mangila.app.shared.persistence.type.Status;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.server.runner.ThreadLocalJobContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Component
public class ActorKafkaDestinationJobHandler implements JobRequestHandler<ActorKafkaDestinationJobRequest> {

    private static final Logger log = new JobRunrDashboardLogger(
            LoggerFactory.getLogger(ActorKafkaDestinationJobHandler.class));

    private final TransactionTemplate transactionTemplate;

    private final ActorOutboxDestinationService destinationService;

    public ActorKafkaDestinationJobHandler(TransactionTemplate transactionTemplate,
                                           ActorOutboxDestinationService destinationService) {
        this.transactionTemplate = transactionTemplate;
        this.destinationService = destinationService;
    }

    @Override
    public void run(ActorKafkaDestinationJobRequest jobRequest) throws Exception {
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
            TimeUnit.SECONDS.sleep(ThreadLocalRandom.current().nextInt(1, 5));
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
