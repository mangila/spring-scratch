package com.github.mangila.app.movie.scheduler.outbox.shared;

import com.github.mangila.app.movie.scheduler.outbox.shared.result.ClaimBatchStepResult;
import com.github.mangila.app.movie.service.MovieOutboxService;
import com.github.mangila.app.shared.persistence.type.Status;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;

@Component
public class ClaimBatchStepHandler {

    private static final Logger log = new JobRunrDashboardLogger(
            LoggerFactory.getLogger(ClaimBatchStepHandler.class));

    private final TransactionTemplate transactionTemplate;

    private final MovieOutboxService movieOutboxService;

    public ClaimBatchStepHandler(TransactionTemplate transactionTemplate, MovieOutboxService movieOutboxService) {
        this.transactionTemplate = transactionTemplate;
        this.movieOutboxService = movieOutboxService;
    }

    @Retryable
    public ClaimBatchStepResult handle(Status from, Status to, int limit) {
        try {
            var l = transactionTemplate.execute(_ -> movieOutboxService.claimBatch(from, to, limit));
            Objects.requireNonNull(l, "claimBatch returned null");
            return new ClaimBatchStepResult(l);
        } catch (Exception e) {
            log.error("Error while claiming outbox batch: {}", e.getMessage(), e);
            throw e;
        }
    }
}
