package com.github.mangila.app.movie.scheduler.outbox.process;

import com.github.mangila.app.movie.scheduler.outbox.process.step.ChangeStatusStepHandler;
import com.github.mangila.app.movie.scheduler.outbox.process.step.CreateDestinationStepHandler;
import com.github.mangila.app.movie.scheduler.outbox.process.step.ScheduleDestinationStepHandler;
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
    private final ChangeStatusStepHandler changeStatusStepHandler;
    private final CreateDestinationStepHandler createDestinationStepHandler;
    private final ScheduleDestinationStepHandler scheduleDestinationStepHandler;

    public MovieOutboxProcessJobHandler(JsonMapper jsonMapper,
                                        MovieOutboxService movieOutboxService, MovieOutboxVersionService movieOutboxVersionService, ChangeStatusStepHandler changeStatusStepHandler,
                                        CreateDestinationStepHandler createDestinationStepHandler,
                                        ScheduleDestinationStepHandler scheduleDestinationStepHandler) {
        this.jsonMapper = jsonMapper;
        this.movieOutboxService = movieOutboxService;
        this.movieOutboxVersionService = movieOutboxVersionService;
        this.changeStatusStepHandler = changeStatusStepHandler;
        this.createDestinationStepHandler = createDestinationStepHandler;
        this.scheduleDestinationStepHandler = scheduleDestinationStepHandler;
    }

    @Override
    public void run(MovieOutboxProcessJobRequest jobRequest) throws Exception {
        final var context = ThreadLocalJobContext.getJobContext();
        final var outboxId = jobRequest.outboxId();

        String outboxJson = context.runStepOnce("outbox", () -> {
            log.info("Fetching outbox: {}", outboxId);
            var outbox = movieOutboxService.findById(outboxId);
            return jsonMapper.writeValueAsString(outbox);
        });

        var outbox = jsonMapper.readValue(outboxJson, OutboxProjection.class);
        final boolean canProcess = movieOutboxVersionService.canProcess(outbox.aggregateId(), outbox.aggregateVersion());
        if (!canProcess) {
            throw new IllegalStateException("Version mismatch %s: %s - %s".formatted(outboxId, outbox.aggregateVersion(), outbox.aggregateId()));
        }

        boolean ok = context.runStepOnce("status", () -> {
            log.info("Changing status of outbox: {} from {} to {}", outboxId, CLAIMED, PROCESSING);
            return changeStatusStepHandler.handle(outboxId, CLAIMED, PROCESSING);
        });

        if (!ok) {
            log.info("Outbox: {} failed to change status from {} to {}", outboxId, CLAIMED, PROCESSING);
            return;
        }

        context.runStepOnce("destination", () -> {
            log.info("Creating destinations for outbox: {}", outboxId);
            var destinationIds = createDestinationStepHandler.handle(outboxId);
            log.info("Created destinations for outbox: {} - {}", outboxId, destinationIds);
        });


        context.runStepOnce("schedule", () -> {
            var jobId = scheduleDestinationStepHandler.handle(outboxId);
            log.info("Scheduled outbox destinations: {} - jobId: {}", outboxId, jobId.asUUID());
        });
    }
}
