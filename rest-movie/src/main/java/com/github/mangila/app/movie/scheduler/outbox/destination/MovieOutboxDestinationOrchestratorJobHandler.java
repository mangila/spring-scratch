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

        for (var outboxDestination : destinations) {
            final var destination = outboxDestination.destination();
            context.runStepOnce(destination.toString(), () -> {
                log.info("Scheduling destination: {} - {}", outboxDestination.id(), destination);
                var jobId = scheduleDestinationStep.execute(outboxDestination);
                log.info("Scheduled destination: {} - {} - {}", outboxDestination.id(), destination, jobId);
            });
        }
    }

}