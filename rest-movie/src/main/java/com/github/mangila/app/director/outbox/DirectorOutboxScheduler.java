package com.github.mangila.app.director.outbox;

import com.github.mangila.app.director.outbox.destination.DirectorOutboxDestinationOrchestratorJobRequest;
import com.github.mangila.app.director.outbox.destination.http.DirectorHttpDestinationJobRequest;
import com.github.mangila.app.director.outbox.destination.kafka.DirectorKafkaDestinationJobRequest;
import com.github.mangila.app.director.outbox.monitor.DirectorOutboxMonitorJobRequest;
import com.github.mangila.app.director.outbox.process.DirectorOutboxProcessJobRequest;
import com.github.mangila.app.director.outbox.purge.DirectorOutboxPurgeJobRequest;
import com.github.mangila.app.director.outbox.recover.DirectorOutboxRecoverJobRequest;
import com.github.mangila.app.director.outbox.relay.DirectorOutboxRelayJobRequest;
import com.github.mangila.app.shared.chaos.Chaos;
import org.jobrunr.jobs.JobId;
import org.jobrunr.scheduling.JobBuilder;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class DirectorOutboxScheduler {

    private final JobRequestScheduler jobRequestScheduler;

    public DirectorOutboxScheduler(JobRequestScheduler jobRequestScheduler) {
        this.jobRequestScheduler = jobRequestScheduler;
    }

    @Chaos
    public JobId schedule(DirectorOutboxRelayJobRequest request) {
        var job = JobBuilder.aJob()
                .scheduleIn(Duration.ofSeconds(1))
                .withName("Director outbox relay")
                .withJobRequest(request)
                .withLabels("director", "outbox", "relay")
                .withAmountOfRetries(10);
        return jobRequestScheduler.create(job);
    }

    @Chaos
    public JobId schedule(DirectorOutboxMonitorJobRequest request) {
        var job = JobBuilder.aJob()
                .scheduleIn(Duration.ofSeconds(1))
                .withName("Director outbox monitor")
                .withJobRequest(request)
                .withLabels("director", "outbox", "monitor")
                .withAmountOfRetries(10);
        return jobRequestScheduler.create(job);
    }

    @Chaos
    public JobId schedule(DirectorOutboxPurgeJobRequest request) {
        var job = JobBuilder.aJob()
                .scheduleIn(Duration.ofSeconds(1))
                .withName("Director outbox purge")
                .withJobRequest(request)
                .withLabels("director", "outbox", "purge")
                .withAmountOfRetries(10);
        return jobRequestScheduler.create(job);
    }

    @Chaos
    public JobId schedule(DirectorOutboxRecoverJobRequest request) {
        var job = JobBuilder.aJob()
                .scheduleIn(Duration.ofSeconds(1))
                .withName("Director outbox recover")
                .withJobRequest(request)
                .withLabels("director", "outbox", "recover")
                .withAmountOfRetries(10);
        return jobRequestScheduler.create(job);
    }

    @Chaos
    public JobId schedule(DirectorHttpDestinationJobRequest request) {
        final var destinationId = request.destinationId();
        final var destination = request.destination();
        var job = JobBuilder.aJob()
                .scheduleIn(Duration.ofSeconds(1))
                .withName("%s: %s".formatted(destinationId, destination))
                .withJobRequest(request)
                .withLabels("director", "outbox", "destination")
                .withAmountOfRetries(10);
        return jobRequestScheduler.create(job);
    }

    @Chaos
    public JobId schedule(DirectorKafkaDestinationJobRequest request) {
        final var destinationId = request.destinationId();
        final var destination = request.destination();
        var job = JobBuilder.aJob()
                .scheduleIn(Duration.ofSeconds(1))
                .withName("%s: %s".formatted(destinationId, destination))
                .withJobRequest(request)
                .withLabels("director", "outbox", "destination")
                .withAmountOfRetries(10);
        return jobRequestScheduler.create(job);
    }

    @Chaos
    public JobId schedule(DirectorOutboxProcessJobRequest request) {
        final var outboxId = request.outboxId();
        var job = JobBuilder.aJob()
                .scheduleIn(Duration.ofSeconds(1))
                .withName(outboxId.toString())
                .withJobRequest(request)
                .withLabels("director", "outbox", "process")
                .withAmountOfRetries(10);
        return jobRequestScheduler.create(job);
    }

    @Chaos
    public JobId schedule(DirectorOutboxDestinationOrchestratorJobRequest request) {
        final var outboxId = request.outboxId();
        var job = JobBuilder.aJob()
                .scheduleIn(Duration.ofSeconds(1))
                .withName(outboxId.toString())
                .withJobRequest(request)
                .withLabels("director", "outbox", "orchestrator")
                .withAmountOfRetries(10);
        return jobRequestScheduler.create(job);
    }
}
