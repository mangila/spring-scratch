package com.github.mangila.app.actor.outbox;

import com.github.mangila.app.actor.outbox.destination.ActorOutboxDestinationOrchestratorJobRequest;
import com.github.mangila.app.actor.outbox.destination.http.ActorHttpDestinationJobRequest;
import com.github.mangila.app.actor.outbox.destination.kafka.ActorKafkaDestinationJobRequest;
import com.github.mangila.app.actor.outbox.monitor.ActorOutboxMonitorJobRequest;
import com.github.mangila.app.actor.outbox.process.ActorOutboxProcessJobRequest;
import com.github.mangila.app.actor.outbox.purge.ActorOutboxPurgeJobRequest;
import com.github.mangila.app.actor.outbox.recover.ActorOutboxRecoverJobRequest;
import com.github.mangila.app.actor.outbox.relay.ActorOutboxRelayJobRequest;
import com.github.mangila.app.shared.chaos.Chaos;
import org.jobrunr.jobs.JobId;
import org.jobrunr.scheduling.JobBuilder;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class ActorOutboxScheduler {

    private final JobRequestScheduler jobRequestScheduler;

    public ActorOutboxScheduler(JobRequestScheduler jobRequestScheduler) {
        this.jobRequestScheduler = jobRequestScheduler;
    }

    @Chaos
    public JobId schedule(ActorOutboxRelayJobRequest request) {
        var job = JobBuilder.aJob()
                .scheduleIn(Duration.ofSeconds(1))
                .withName("Actor outbox relay")
                .withJobRequest(request)
                .withLabels("actor", "outbox", "relay")
                .withAmountOfRetries(10);
        return jobRequestScheduler.create(job);
    }

    @Chaos
    public JobId schedule(ActorOutboxMonitorJobRequest request) {
        var job = JobBuilder.aJob()
                .scheduleIn(Duration.ofSeconds(1))
                .withName("Actor outbox monitor")
                .withJobRequest(request)
                .withLabels("actor", "outbox", "monitor")
                .withAmountOfRetries(10);
        return jobRequestScheduler.create(job);
    }

    @Chaos
    public JobId schedule(ActorOutboxPurgeJobRequest request) {
        var job = JobBuilder.aJob()
                .scheduleIn(Duration.ofSeconds(1))
                .withName("Actor outbox purge")
                .withJobRequest(request)
                .withLabels("actor", "outbox", "purge")
                .withAmountOfRetries(10);
        return jobRequestScheduler.create(job);
    }

    @Chaos
    public JobId schedule(ActorOutboxRecoverJobRequest request) {
        var job = JobBuilder.aJob()
                .scheduleIn(Duration.ofSeconds(1))
                .withName("Actor outbox recover")
                .withJobRequest(request)
                .withLabels("actor", "outbox", "recover")
                .withAmountOfRetries(10);
        return jobRequestScheduler.create(job);
    }

    @Chaos
    public JobId schedule(ActorHttpDestinationJobRequest request) {
        final var destinationId = request.destinationId();
        final var destination = request.destination();
        var job = JobBuilder.aJob()
                .scheduleIn(Duration.ofSeconds(1))
                .withName("%s: %s".formatted(destinationId, destination))
                .withJobRequest(request)
                .withLabels("actor", "outbox", "destination")
                .withAmountOfRetries(10);
        return jobRequestScheduler.create(job);
    }

    @Chaos
    public JobId schedule(ActorKafkaDestinationJobRequest request) {
        final var destinationId = request.destinationId();
        final var destination = request.destination();
        var job = JobBuilder.aJob()
                .scheduleIn(Duration.ofSeconds(1))
                .withName("%s: %s".formatted(destinationId, destination))
                .withJobRequest(request)
                .withLabels("actor", "outbox", "destination")
                .withAmountOfRetries(10);
        return jobRequestScheduler.create(job);
    }

    @Chaos
    public JobId schedule(ActorOutboxProcessJobRequest request) {
        final var outboxId = request.outboxId();
        var job = JobBuilder.aJob()
                .scheduleIn(Duration.ofSeconds(1))
                .withName(outboxId.toString())
                .withJobRequest(request)
                .withLabels("actor", "outbox", "process")
                .withAmountOfRetries(10);
        return jobRequestScheduler.create(job);
    }

    @Chaos
    public JobId schedule(ActorOutboxDestinationOrchestratorJobRequest request) {
        final var outboxId = request.outboxId();
        var job = JobBuilder.aJob()
                .scheduleIn(Duration.ofSeconds(1))
                .withName(outboxId.toString())
                .withJobRequest(request)
                .withLabels("actor", "outbox", "orchestrator")
                .withAmountOfRetries(10);
        return jobRequestScheduler.create(job);
    }
}
