package com.github.mangila.app.movie.scheduler.outbox.destination;

import com.github.mangila.app.movie.scheduler.outbox.destination.step.ScheduleDestinationStep;
import com.github.mangila.app.movie.service.MovieOutboxDestinationService;
import com.github.mangila.app.shared.persistence.type.Status;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.server.runner.ThreadLocalJobContext;
import org.jobrunr.utils.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.UUID;

@Component
public class MovieOutboxDestinationOrchestratorJobHandler implements JobRequestHandler<MovieOutboxDestinationOrchestratorJobRequest> {

    private static final Logger log = new JobRunrDashboardLogger(
            LoggerFactory.getLogger(MovieOutboxDestinationOrchestratorJobHandler.class));

    private final ScheduleDestinationStep scheduleDestinationStep;
    private final MovieOutboxDestinationService destinationService;

    public MovieOutboxDestinationOrchestratorJobHandler(ScheduleDestinationStep scheduleDestinationStep,
                                                        MovieOutboxDestinationService destinationService) {
        this.scheduleDestinationStep = scheduleDestinationStep;
        this.destinationService = destinationService;
    }

    @Override
    public void run(MovieOutboxDestinationOrchestratorJobRequest jobRequest) throws Exception {
        final var context = ThreadLocalJobContext.getJobContext();
        final var outboxId = jobRequest.outboxId();

        var destinations = destinationService.findAllByOutboxIdAndStatus(outboxId, Status.PENDING);

        if (CollectionUtils.isNullOrEmpty(destinations)) {
            log.info("No destinations found for outbox: {}", outboxId);
            return;
        }

        var errors = new ArrayList<UUID>();
        for (var outboxDestination : destinations) {
            final var destinationId = outboxDestination.id();
            final var destination = outboxDestination.destination();
            try {
                context.runStepOnce(destination.toString(), () -> {
                    var jobId = scheduleDestinationStep.execute(outboxDestination);
                    log.info("outbox id: {} scheduled destination id: {} send to: {} jobId: {}", outboxId, destinationId, destination, jobId);
                });
            } catch (Exception e) {
                log.error("Error scheduling destination id: {} - {}", destinationId, e.getMessage(), e);
                errors.add(destinationId);
            }
        }

        if (CollectionUtils.isNotNullOrEmpty(errors)) {
            throw new IllegalStateException("Failed to schedule destinations: %s".formatted(errors));
        }
    }

}