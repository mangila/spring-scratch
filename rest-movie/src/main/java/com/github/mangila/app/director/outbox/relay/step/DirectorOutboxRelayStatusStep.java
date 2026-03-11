package com.github.mangila.app.director.outbox.relay.step;
 
import com.github.mangila.app.director.service.DirectorOutboxService;
import com.github.mangila.app.shared.persistence.type.Status;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
 
import java.util.UUID;
 
@Component
public class DirectorOutboxRelayStatusStep {
 
    private static final Logger log = new JobRunrDashboardLogger(
            LoggerFactory.getLogger(DirectorOutboxRelayStatusStep.class));
 
    private final TransactionTemplate transactionTemplate;
    private final DirectorOutboxService directorOutboxService;
 
    public DirectorOutboxRelayStatusStep(TransactionTemplate transactionTemplate,
                                      DirectorOutboxService directorOutboxService) {
        this.transactionTemplate = transactionTemplate;
        this.directorOutboxService = directorOutboxService;
    }
 
    @Retryable
    public boolean execute(UUID outboxId, Status from, Status to) {
        try {
            return transactionTemplate.execute(_ -> directorOutboxService.changeStatus(outboxId, from, to));
        } catch (Exception e) {
            log.error("Error while changing status for outbox: {} - {}", outboxId, e.getMessage(), e);
            throw e;
        }
    }
 
}
