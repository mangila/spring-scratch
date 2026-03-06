package com.github.mangila.app.movie.scheduler.outbox.process;

import com.github.mangila.app.movie.scheduler.outbox.process.step.ChangeStatusStep;
import com.github.mangila.app.movie.scheduler.outbox.process.step.CreateDestinationStep;
import com.github.mangila.app.movie.scheduler.outbox.process.step.ScheduleDestinationOrchestratorStep;
import com.github.mangila.app.movie.service.MovieOutboxService;
import com.github.mangila.app.movie.service.MovieOutboxVersionService;
import com.github.mangila.app.shared.persistence.base.projection.OutboxProjection;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.server.runner.ThreadLocalJobContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import static com.github.mangila.app.shared.persistence.type.Status.CLAIMED;
import static com.github.mangila.app.shared.persistence.type.Status.PROCESSING;

@Component
public class MovieOutboxProcessJobHandler implements JobRequestHandler<MovieOutboxProcessJobRequest> {

    private static final Logger log = new JobRunrDashboardLogger(
            LoggerFactory.getLogger(MovieOutboxProcessJobHandler.class));

    private final JsonMapper jsonMapper;
    private final MovieOutboxService movieOutboxService;
    private final MovieOutboxVersionService movieOutboxVersionService;
    private final ChangeStatusStep changeStatusStep;
    private final CreateDestinationStep createDestinationStep;
    private final ScheduleDestinationOrchestratorStep scheduleDestinationOrchestratorStep;

    public MovieOutboxProcessJobHandler(JsonMapper jsonMapper,
                                        MovieOutboxService movieOutboxService, MovieOutboxVersionService movieOutboxVersionService, ChangeStatusStep changeStatusStep,
                                        CreateDestinationStep createDestinationStep,
                                        ScheduleDestinationOrchestratorStep scheduleDestinationOrchestratorStep) {
        this.jsonMapper = jsonMapper;
        this.movieOutboxService = movieOutboxService;
        this.movieOutboxVersionService = movieOutboxVersionService;
        this.changeStatusStep = changeStatusStep;
        this.createDestinationStep = createDestinationStep;
        this.scheduleDestinationOrchestratorStep = scheduleDestinationOrchestratorStep;
    }

    @Override
    public void run(MovieOutboxProcessJobRequest jobRequest) throws Exception {
        final var context = ThreadLocalJobContext.getJobContext();
        final var outboxId = jobRequest.outboxId();

        String outboxJson = context.runStepOnce("outbox", () -> {
            var outbox = movieOutboxService.findById(outboxId);
            log.info("Fetched outbox: {}", outboxId);
            return jsonMapper.writeValueAsString(outbox);
        });

        var outbox = jsonMapper.readValue(outboxJson, OutboxProjection.class);

        final boolean canProcess = movieOutboxVersionService.canProcess(outbox.aggregateId(), outbox.aggregateVersion());
        if (!canProcess) {
            throw new IllegalStateException("Version mismatch %s: %s - %s".formatted(outboxId, outbox.aggregateVersion(), outbox.aggregateId()));
        }

        boolean ok = context.runStepOnce("status", () -> {
            final boolean execute = changeStatusStep.execute(outboxId, CLAIMED, PROCESSING);
            log.info("Changed status of outbox: {} from {} to {}", outboxId, CLAIMED, PROCESSING);
            return execute;
        });

        if (!ok) {
            log.info("Outbox: {} failed to change status from {} to {}", outboxId, CLAIMED, PROCESSING);
            return;
        }

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
