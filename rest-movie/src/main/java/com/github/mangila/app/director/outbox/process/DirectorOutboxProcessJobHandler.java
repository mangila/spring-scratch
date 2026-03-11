package com.github.mangila.app.director.outbox.process;

import com.github.mangila.app.director.outbox.process.step.DirectorOutboxProcessDestinationStep;
import com.github.mangila.app.director.outbox.process.step.DirectorOutboxProcessScheduleStep;
import com.github.mangila.app.director.outbox.process.step.DirectorOutboxProcessStatusStep;
import com.github.mangila.app.director.service.DirectorOutboxService;
import com.github.mangila.app.director.service.DirectorOutboxVersionService;
import com.github.mangila.app.shared.persistence.type.Status;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.server.runner.ThreadLocalJobContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DirectorOutboxProcessJobHandler implements JobRequestHandler<DirectorOutboxProcessJobRequest> {

    private static final Logger log = new JobRunrDashboardLogger(
            LoggerFactory.getLogger(DirectorOutboxProcessJobHandler.class));

    private final DirectorOutboxService directorOutboxService;
    private final DirectorOutboxVersionService directorOutboxVersionService;
    private final DirectorOutboxProcessStatusStep directorOutboxProcessStatusStep;
    private final DirectorOutboxProcessDestinationStep directorOutboxProcessDestinationStep;
    private final DirectorOutboxProcessScheduleStep directorOutboxProcessScheduleStep;

    public DirectorOutboxProcessJobHandler(DirectorOutboxService directorOutboxService,
                                        DirectorOutboxVersionService directorOutboxVersionService,
                                        DirectorOutboxProcessStatusStep directorOutboxProcessStatusStep,
                                        DirectorOutboxProcessDestinationStep directorOutboxProcessDestinationStep,
                                        DirectorOutboxProcessScheduleStep directorOutboxProcessScheduleStep) {
        this.directorOutboxService = directorOutboxService;
        this.directorOutboxVersionService = directorOutboxVersionService;
        this.directorOutboxProcessStatusStep = directorOutboxProcessStatusStep;
        this.directorOutboxProcessDestinationStep = directorOutboxProcessDestinationStep;
        this.directorOutboxProcessScheduleStep = directorOutboxProcessScheduleStep;
    }

    @Override
    public void run(DirectorOutboxProcessJobRequest jobRequest) throws Exception {
        final var context = ThreadLocalJobContext.getJobContext();
        final var outboxId = jobRequest.outboxId();

        final var outbox = directorOutboxService.findById(outboxId);
        log.info("Fetched outbox: {}", outboxId);

        final boolean canProcess = directorOutboxVersionService.canProcess(outbox.aggregateId(), outbox.aggregateVersion());
        if (!canProcess) {
            throw new IllegalStateException("Version mismatch %s: %s - %s".formatted(outboxId, outbox.aggregateVersion(), outbox.aggregateId()));
        }

        context.runStepOnce("status", () -> {
            final var fromStatus = Status.SCHEDULED;
            final var toStatus = Status.PROCESSING;
            final boolean execute = directorOutboxProcessStatusStep.execute(outboxId, fromStatus, toStatus);
            if (!execute) {
                throw new IllegalStateException("Outbox: %s failed to change status from %s to %s".formatted(outboxId, fromStatus, toStatus));
            }
            log.info("Changed status of outbox: {} from {} to {}", outboxId, fromStatus, toStatus);
        });

        context.runStepOnce("destination", () -> {
            var destinationIds = directorOutboxProcessDestinationStep.execute(outboxId);
            log.info("Created destinations for outbox: {} - {}", outboxId, destinationIds);
        });

        context.runStepOnce("schedule", () -> {
            var jobId = directorOutboxProcessScheduleStep.execute(outboxId);
            log.info("Scheduled destination orchestrator for outbox: {} - jobId: {}", outboxId, jobId);
        });
    }
}
