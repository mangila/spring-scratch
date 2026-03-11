package com.github.mangila.app.director.outbox.monitor;

import com.github.mangila.app.director.service.DirectorOutboxDestinationService;
import com.github.mangila.app.director.service.DirectorOutboxService;
import com.github.mangila.app.director.service.DirectorOutboxVersionService;
import com.github.mangila.app.shared.persistence.type.Status;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.server.runner.ThreadLocalJobContext;
import org.jobrunr.utils.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.UUID;

@Component
public class DirectorOutboxMonitorJobHandler implements JobRequestHandler<DirectorOutboxMonitorJobRequest> {

    private static final Logger log = new JobRunrDashboardLogger(
            LoggerFactory.getLogger(DirectorOutboxMonitorJobHandler.class));

    private final TransactionTemplate transactionTemplate;
    private final DirectorOutboxVersionService directorOutboxVersionService;
    private final DirectorOutboxService directorOutboxService;
    private final DirectorOutboxDestinationService directorOutboxDestinationService;

    public DirectorOutboxMonitorJobHandler(TransactionTemplate transactionTemplate,
                                        DirectorOutboxVersionService directorOutboxVersionService,
                                        DirectorOutboxService directorOutboxService,
                                        DirectorOutboxDestinationService directorOutboxDestinationService) {
        this.transactionTemplate = transactionTemplate;
        this.directorOutboxVersionService = directorOutboxVersionService;
        this.directorOutboxService = directorOutboxService;
        this.directorOutboxDestinationService = directorOutboxDestinationService;
    }

    @Override
    public void run(DirectorOutboxMonitorJobRequest jobRequest) throws Exception {
        final var context = ThreadLocalJobContext.getJobContext();
        final var limit = jobRequest.limit();

        var outboxes = directorOutboxService.findAllByStatus(Status.PROCESSING, limit);
        log.info("Found {} outboxes to monitor", outboxes.size());

        var errors = new ArrayList<UUID>(outboxes.size());
        for (var outbox : outboxes) {
            final var outboxId = outbox.id();
            final var aggregateId = outbox.aggregateId();
            try {
                var destinationEntities = directorOutboxDestinationService.findAllByOutboxId(outboxId);
                if (destinationEntities.isEmpty()) {
                    log.info("No destinations for outbox: {}", outboxId);
                    continue;
                }
                log.info("Found {} destinations for outbox: {}", destinationEntities.size(), outboxId);
                var allSuccessMatch = destinationEntities.stream()
                        .allMatch(destinationEntity -> destinationEntity.status() == Status.SUCCESS);
                if (allSuccessMatch) {
                    final var fromStatus = Status.PROCESSING;
                    final var toStatus = Status.SUCCESS;
                    transactionTemplate.executeWithoutResult(_ -> {
                        final boolean ok = directorOutboxService.changeStatus(outboxId, fromStatus, toStatus);
                        if (!ok) {
                            throw new IllegalStateException("Outbox: %s failed to change status from %s to %s".formatted(outboxId, fromStatus, toStatus));
                        }
                        directorOutboxVersionService.increment(aggregateId);
                        log.info("Outbox: {} changed status from {} to {} and bumped version on aggregate: {}", outboxId, fromStatus, toStatus, aggregateId);
                    });
                }
            } catch (Exception e) {
                log.error("Error while monitoring outbox: {} - {}", outboxId, e.getMessage(), e);
                errors.add(outboxId);
            }
        }

        if (CollectionUtils.isNotNullOrEmpty(errors)) {
            throw new IllegalStateException("Failed to monitor outboxes: %s".formatted(errors));
        }

    }
}
