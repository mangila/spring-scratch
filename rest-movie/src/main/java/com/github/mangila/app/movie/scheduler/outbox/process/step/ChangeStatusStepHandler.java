package com.github.mangila.app.movie.scheduler.outbox.process.step;

import com.github.mangila.app.movie.service.MovieOutboxService;
import com.github.mangila.app.shared.persistence.type.Status;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

@Component
public class ChangeStatusStepHandler {

    private static final Logger log = new JobRunrDashboardLogger(
            LoggerFactory.getLogger(ChangeStatusStepHandler.class));

    private final TransactionTemplate transactionTemplate;
    private final MovieOutboxService movieOutboxService;

    public ChangeStatusStepHandler(TransactionTemplate transactionTemplate,
                                   MovieOutboxService movieOutboxService) {
        this.transactionTemplate = transactionTemplate;
        this.movieOutboxService = movieOutboxService;
    }

    @Retryable
    public boolean handle(UUID outboxId, Status to, Status from) {
        try {
            return transactionTemplate.execute(_ -> movieOutboxService.changeStatus(outboxId, to, from));
        } catch (Exception e) {
            log.error("Error while changing status for outbox: {} - {}", outboxId, e.getMessage(), e);
            throw e;
        }
    }
}
