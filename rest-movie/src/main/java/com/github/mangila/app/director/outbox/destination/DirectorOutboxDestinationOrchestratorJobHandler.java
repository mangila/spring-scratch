package com.github.mangila.app.director.outbox.destination;

import com.github.mangila.app.director.outbox.destination.step.DirectorOutboxDestinationOrchestratorClaimStep;
import com.github.mangila.app.director.outbox.destination.step.DirectorOutboxDestinationOrchestratorScheduleStep;
import com.github.mangila.app.shared.persistence.base.projection.OutboxDestinationProjection;
import com.github.mangila.app.shared.persistence.type.Status;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.server.runner.ThreadLocalJobContext;
import org.jobrunr.utils.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.UUID;

@Component
public class DirectorOutboxDestinationOrchestratorJobHandler implements JobRequestHandler<DirectorOutboxDestinationOrchestratorJobRequest> {

    private static final Logger log = new JobRunrDashboardLogger(
            LoggerFactory.getLogger(DirectorOutboxDestinationOrchestratorJobHandler.class));

    private final JsonMapper jsonMapper;
    private final DirectorOutboxDestinationOrchestratorClaimStep directorOutboxDestinationOrchestratorClaimStep;
    private final DirectorOutboxDestinationOrchestratorScheduleStep directorOutboxDestinationOrchestratorScheduleStep;

    public DirectorOutboxDestinationOrchestratorJobHandler(JsonMapper jsonMapper,
                                                        DirectorOutboxDestinationOrchestratorClaimStep directorOutboxDestinationOrchestratorClaimStep,
                                                        DirectorOutboxDestinationOrchestratorScheduleStep directorOutboxDestinationOrchestratorScheduleStep) {
        this.jsonMapper = jsonMapper;
        this.directorOutboxDestinationOrchestratorClaimStep = directorOutboxDestinationOrchestratorClaimStep;
        this.directorOutboxDestinationOrchestratorScheduleStep = directorOutboxDestinationOrchestratorScheduleStep;
    }

    @Override
    public void run(DirectorOutboxDestinationOrchestratorJobRequest jobRequest) throws Exception {
        final var context = ThreadLocalJobContext.getJobContext();
        final var outboxId = jobRequest.outboxId();

        String destinationProjections = context.runStepOnce("claim", () -> {
            final var fromStatus = Status.PENDING;
            final var toStatus = Status.CLAIMED;
            return directorOutboxDestinationOrchestratorClaimStep.execute(outboxId, fromStatus, toStatus);
        });

        var destinations = jsonMapper.readValue(destinationProjections, OutboxDestinationProjection[].class);

        if (CollectionUtils.isNullOrEmpty(destinations)) {
            log.info("No destinations found for outbox: {}", outboxId);
            return;
        }

        log.info("Scheduling {} destinations for outbox: {}", destinations.length, outboxId);
        var errors = new ArrayList<UUID>(destinations.length);
        for (var outboxDestination : destinations) {
            final var destinationId = outboxDestination.id();
            final var destination = outboxDestination.destination();
            try {
                context.runStepOnce("schedule:" + destination.toString(), () -> {
                    var jobId = directorOutboxDestinationOrchestratorScheduleStep.execute(outboxDestination);
                    log.info("outbox id: {} scheduled destination id: {} send to: {} jobId: {}", outboxId, destinationId, destination, jobId);
                });
            } catch (Exception e) {
                log.error("Error scheduling destination id: {} - {}", destinationId, e.getMessage(), e);
                errors.add(destinationId);
            }
        }

        if (CollectionUtils.isNotNullOrEmpty(errors)) {
            var errorString = String.join(",", errors.stream()
                    .map(UUID::toString)
                    .toList());
            context.saveMetadata("errors", errorString);
            throw new IllegalStateException("Failed to schedule destinations: %s".formatted(errorString));
        }
    }

}