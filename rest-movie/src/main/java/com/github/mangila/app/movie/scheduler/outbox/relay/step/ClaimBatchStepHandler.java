package com.github.mangila.app.movie.scheduler.outbox.relay.step;

import com.github.mangila.app.movie.service.MovieOutboxService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;

@Component
public class ClaimBatchStepHandler {

    private final TransactionTemplate transactionTemplate;
    private final MovieOutboxService movieOutboxService;

    public ClaimBatchStepHandler(TransactionTemplate transactionTemplate,
                                 MovieOutboxService movieOutboxService) {
        this.transactionTemplate = transactionTemplate;
        this.movieOutboxService = movieOutboxService;
    }

    public ClaimBatchStepResult handle(int limit) {
        var l = transactionTemplate.execute(_ -> movieOutboxService.claimOutboxPending(limit));
        Objects.requireNonNull(l, "claimOutboxPending returned null");
        return new ClaimBatchStepResult(l);
    }

}
