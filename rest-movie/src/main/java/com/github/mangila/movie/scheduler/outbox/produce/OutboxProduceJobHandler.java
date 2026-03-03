package com.github.mangila.movie.scheduler.outbox.produce;

import com.github.mangila.movie.persistence.outbox.version.OutboxVersionJpaRepository;
import com.github.mangila.movie.scheduler.outbox.OutboxScheduler;
import com.github.mangila.movie.scheduler.outbox.produce.destination.ProduceKafkaJobRequest;
import com.github.mangila.movie.scheduler.outbox.produce.destination.ProduceRabbitMqJobRequest;
import com.github.mangila.movie.scheduler.outbox.produce.destination.ProduceSqsJobRequest;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.server.runner.ThreadLocalJobContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;

@Component
public class OutboxProduceJobHandler implements JobRequestHandler<OutboxProduceJobRequest> {

    private final OutboxScheduler outboxScheduler;
    private final TransactionTemplate transactionTemplate;
    private final OutboxVersionJpaRepository outboxVersionJpaRepository;

    public OutboxProduceJobHandler(OutboxScheduler outboxScheduler, TransactionTemplate transactionTemplate, OutboxVersionJpaRepository outboxVersionJpaRepository) {
        this.outboxScheduler = outboxScheduler;
        this.transactionTemplate = transactionTemplate;
        this.outboxVersionJpaRepository = outboxVersionJpaRepository;
    }

    @Override
    public void run(OutboxProduceJobRequest jobRequest) throws Exception {
        final var outbox = jobRequest.outbox();
        final var context = ThreadLocalJobContext.getJobContext();
        transactionTemplate.executeWithoutResult(_ -> {
            final var aggregateId = outbox.aggregateId();
            final var version = outbox.version();
            final var versionEntity = outboxVersionJpaRepository.findByAggregateIdLocked(aggregateId)
                    .orElseThrow();
            final var currentVersion = versionEntity.getCurrentVersion();
            if (Objects.equals(currentVersion, version)) {
                context.runStepOnce("kafka", () -> {
                    outboxScheduler.schedule(new ProduceKafkaJobRequest(outbox));
                });
                context.runStepOnce("rabbitmq", () -> {
                    outboxScheduler.schedule(new ProduceRabbitMqJobRequest(outbox));
                });
                context.runStepOnce("sqs", () -> {
                    outboxScheduler.schedule(new ProduceSqsJobRequest(outbox));
                });
            }
        });
    }
}
