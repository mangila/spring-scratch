package com.github.mangila.app.actor.outbox.relay.step;

import com.github.mangila.app.actor.outbox.ActorOutboxScheduler;
import com.github.mangila.app.actor.outbox.process.ActorOutboxProcessJobRequest;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ActorOutboxRelayScheduleStep {

    private static final Logger log = new JobRunrDashboardLogger(
            LoggerFactory.getLogger(ActorOutboxRelayScheduleStep.class));

    private final ActorOutboxScheduler actorOutboxScheduler;

    public ActorOutboxRelayScheduleStep(ActorOutboxScheduler actorOutboxScheduler) {
        this.actorOutboxScheduler = actorOutboxScheduler;
    }

    @Retryable
    public UUID execute(UUID outboxId) {
        try {
            var jobId = actorOutboxScheduler.schedule(new ActorOutboxProcessJobRequest(outboxId));
            return jobId.asUUID();
        } catch (Exception e) {
            log.error("Error while scheduling outbox: {} - {}", outboxId, e.getMessage(), e);
            throw e;
        }
    }
}
