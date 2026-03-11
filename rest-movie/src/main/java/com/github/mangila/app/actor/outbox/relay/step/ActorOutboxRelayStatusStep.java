package com.github.mangila.app.actor.outbox.relay.step;

import com.github.mangila.app.actor.service.ActorOutboxService;
import com.github.mangila.app.shared.persistence.type.Status;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

@Component
public class ActorOutboxRelayStatusStep {

    private static final Logger log = new JobRunrDashboardLogger(
            LoggerFactory.getLogger(ActorOutboxRelayStatusStep.class));

    private final TransactionTemplate transactionTemplate;
    private final ActorOutboxService actorOutboxService;

    public ActorOutboxRelayStatusStep(TransactionTemplate transactionTemplate,
                                      ActorOutboxService actorOutboxService) {
        this.transactionTemplate = transactionTemplate;
        this.actorOutboxService = actorOutboxService;
    }

    @Retryable
    public boolean execute(UUID outboxId, Status from, Status to) {
        try {
            return transactionTemplate.execute(_ -> actorOutboxService.changeStatus(outboxId, from, to));
        } catch (Exception e) {
            log.error("Error while changing status for outbox: {} - {}", outboxId, e.getMessage(), e);
            throw e;
        }
    }

}
