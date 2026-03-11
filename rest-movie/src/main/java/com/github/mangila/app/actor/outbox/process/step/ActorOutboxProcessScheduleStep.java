package com.github.mangila.app.actor.outbox.process.step;

import com.github.mangila.app.actor.outbox.ActorOutboxScheduler;
import com.github.mangila.app.actor.outbox.destination.ActorOutboxDestinationOrchestratorJobRequest;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ActorOutboxProcessScheduleStep {

    private static final Logger log = new JobRunrDashboardLogger(
            LoggerFactory.getLogger(ActorOutboxProcessScheduleStep.class));

    private final ActorOutboxScheduler actorOutboxScheduler;

    public ActorOutboxProcessScheduleStep(ActorOutboxScheduler actorOutboxScheduler) {
        this.actorOutboxScheduler = actorOutboxScheduler;
    }

    @Retryable
    public UUID execute(UUID outboxId) {
        try {
            var jobId = actorOutboxScheduler.schedule(new ActorOutboxDestinationOrchestratorJobRequest(outboxId));
            return jobId.asUUID();
        } catch (Exception e) {
            log.error("Error scheduling outboxId: {} to orchestrator: {}", outboxId, e.getMessage(), e);
            throw e;
        }
    }
}
