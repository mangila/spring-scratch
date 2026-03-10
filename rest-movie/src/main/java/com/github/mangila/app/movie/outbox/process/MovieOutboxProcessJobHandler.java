package com.github.mangila.app.movie.outbox.process;

import com.github.mangila.app.movie.outbox.process.step.CreateDestinationStep;
import com.github.mangila.app.movie.outbox.process.step.ScheduleDestinationOrchestratorStep;
import com.github.mangila.app.movie.outbox.shared.ChangeOutboxStatusStep;
import com.github.mangila.app.movie.service.MovieOutboxService;
import com.github.mangila.app.movie.service.MovieOutboxVersionService;
import com.github.mangila.app.shared.persistence.type.Status;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.server.runner.ThreadLocalJobContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MovieOutboxProcessJobHandler implements JobRequestHandler<MovieOutboxProcessJobRequest> {

    private static final Logger log = new JobRunrDashboardLogger(
            LoggerFactory.getLogger(MovieOutboxProcessJobHandler.class));

    private final MovieOutboxService movieOutboxService;
    private final MovieOutboxVersionService movieOutboxVersionService;
    private final ChangeOutboxStatusStep changeOutboxStatusStep;
    private final CreateDestinationStep createDestinationStep;
    private final ScheduleDestinationOrchestratorStep scheduleDestinationOrchestratorStep;

    public MovieOutboxProcessJobHandler(MovieOutboxService movieOutboxService,
                                        MovieOutboxVersionService movieOutboxVersionService,
                                        ChangeOutboxStatusStep changeOutboxStatusStep,
                                        CreateDestinationStep createDestinationStep,
                                        ScheduleDestinationOrchestratorStep scheduleDestinationOrchestratorStep) {
        this.movieOutboxService = movieOutboxService;
        this.movieOutboxVersionService = movieOutboxVersionService;
        this.changeOutboxStatusStep = changeOutboxStatusStep;
        this.createDestinationStep = createDestinationStep;
        this.scheduleDestinationOrchestratorStep = scheduleDestinationOrchestratorStep;
    }

    @Override
    public void run(MovieOutboxProcessJobRequest jobRequest) throws Exception {
        final var context = ThreadLocalJobContext.getJobContext();
        final var outboxId = jobRequest.outboxId();

        final var outbox = movieOutboxService.findById(outboxId);
        log.info("Fetched outbox: {}", outboxId);

        final boolean canProcess = movieOutboxVersionService.canProcess(outbox.aggregateId(), outbox.aggregateVersion());
        if (!canProcess) {
            throw new IllegalStateException("Version mismatch %s: %s - %s".formatted(outboxId, outbox.aggregateVersion(), outbox.aggregateId()));
        }

        final var fromStatus = Status.SCHEDULED;
        final var toStatus = Status.PROCESSING;

        context.runStepOnce("status", () -> {
            final boolean execute = changeOutboxStatusStep.execute(outboxId, fromStatus, toStatus);
            if (!execute) {
                throw new IllegalStateException("Outbox: %s failed to change status from %s to %s".formatted(outboxId, fromStatus, toStatus));
            }
            log.info("Changed status of outbox: {} from {} to {}", outboxId, fromStatus, toStatus);
        });

        context.runStepOnce("destination", () -> {
            var destinationIds = createDestinationStep.execute(outboxId);
            log.info("Created destinations for outbox: {} - {}", outboxId, destinationIds);
        });

        context.runStepOnce("schedule", () -> {
            var jobId = scheduleDestinationOrchestratorStep.execute(outboxId);
            log.info("Scheduled destination orchestrator for outbox: {} - jobId: {}", outboxId, jobId);
        });
    }
}
