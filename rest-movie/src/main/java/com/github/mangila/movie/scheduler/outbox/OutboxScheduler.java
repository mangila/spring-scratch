package com.github.mangila.movie.scheduler.outbox;

import com.github.mangila.movie.properties.OutboxProperties;
import com.github.mangila.movie.scheduler.outbox.produce.OutboxProduceJobRequest;
import com.github.mangila.movie.scheduler.outbox.produce.destination.ProduceKafkaJobRequest;
import com.github.mangila.movie.scheduler.outbox.produce.destination.ProduceRabbitMqJobRequest;
import com.github.mangila.movie.scheduler.outbox.produce.destination.ProduceSqsJobRequest;
import com.github.mangila.movie.scheduler.outbox.relay.OutboxRelayJobRequest;
import org.jobrunr.jobs.JobId;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.jobrunr.scheduling.RecurringJobBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Duration;

import static org.jobrunr.scheduling.JobBuilder.aJob;

@Service
public class OutboxScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxScheduler.class);

    private final OutboxProperties outboxProperties;
    private final JobRequestScheduler jobRequestScheduler;

    public OutboxScheduler(OutboxProperties outboxProperties,
                           JobRequestScheduler jobRequestScheduler) {
        this.outboxProperties = outboxProperties;
        this.jobRequestScheduler = jobRequestScheduler;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        final var enabled = outboxProperties.isEnabled();
        final var cron = outboxProperties.getCron();
        final var limit = outboxProperties.getLimit();
        if (enabled) {
            log.info("Outbox enabled");
            var request = new OutboxRelayJobRequest(limit);
            var job = RecurringJobBuilder.aRecurringJob()
                    .withCron(cron)
                    .withName("Outbox relay")
                    .withJobRequest(request)
                    .withLabels("outbox")
                    .withAmountOfRetries(10);
            jobRequestScheduler.createRecurrently(job);
        }
    }

    public JobId schedule(OutboxProduceJobRequest request) {
        return jobRequestScheduler.create(aJob()
                .scheduleIn(Duration.ofSeconds(1))
                .withName("Outbox produce: %s".formatted(request.outbox().id()))
                .withAmountOfRetries(10)
                .withLabels("outbox")
                .withJobRequest(request));
    }

    public JobId schedule(ProduceKafkaJobRequest request) {
        return jobRequestScheduler.create(aJob()
                .scheduleIn(Duration.ofSeconds(1))
                .withName("Kafka produce: %s".formatted(request.outbox().id()))
                .withAmountOfRetries(10)
                .withLabels("outbox")
                .withJobRequest(request));
    }

    public JobId schedule(ProduceRabbitMqJobRequest request) {
        return jobRequestScheduler.create(aJob()
                .scheduleIn(Duration.ofSeconds(1))
                .withName("RabbitMQ produce: %s".formatted(request.outbox().id()))
                .withAmountOfRetries(10)
                .withLabels("outbox")
                .withJobRequest(request));
    }

    public JobId schedule(ProduceSqsJobRequest request) {
        return jobRequestScheduler.create(aJob()
                .scheduleIn(Duration.ofSeconds(1))
                .withName("AWS SQS produce: %s".formatted(request.outbox().id()))
                .withAmountOfRetries(10)
                .withLabels("outbox")
                .withJobRequest(request));
    }

}
