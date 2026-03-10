package com.github.mangila.app.movie.outbox;

import com.github.mangila.app.movie.outbox.destination.MovieOutboxDestinationOrchestratorJobRequest;
import com.github.mangila.app.movie.outbox.destination.http.MovieHttpDestinationJobRequest;
import com.github.mangila.app.movie.outbox.destination.kafka.MovieKafkaDestinationJobRequest;
import com.github.mangila.app.movie.outbox.monitor.MovieOutboxMonitorJobRequest;
import com.github.mangila.app.movie.outbox.process.MovieOutboxProcessJobRequest;
import com.github.mangila.app.movie.outbox.purge.MovieOutboxPurgeJobRequest;
import com.github.mangila.app.movie.outbox.recover.MovieOutboxRecoverJobRequest;
import com.github.mangila.app.movie.outbox.relay.MovieOutboxRelayJobRequest;
import com.github.mangila.app.shared.chaos.Chaos;
import org.jobrunr.jobs.JobId;
import org.jobrunr.scheduling.JobBuilder;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class MovieOutboxScheduler {

    private final JobRequestScheduler jobRequestScheduler;

    public MovieOutboxScheduler(JobRequestScheduler jobRequestScheduler) {
        this.jobRequestScheduler = jobRequestScheduler;
    }

    @Chaos
    public JobId schedule(MovieOutboxRelayJobRequest request) {
        var job = JobBuilder.aJob()
                .scheduleIn(Duration.ofSeconds(1))
                .withName("Movie outbox relay")
                .withJobRequest(request)
                .withLabels("movie", "outbox", "relay")
                .withAmountOfRetries(10);
        return jobRequestScheduler.create(job);
    }

    @Chaos
    public JobId schedule(MovieOutboxMonitorJobRequest request) {
        var job = JobBuilder.aJob()
                .scheduleIn(Duration.ofSeconds(1))
                .withName("Movie outbox monitor")
                .withJobRequest(request)
                .withLabels("movie", "outbox", "monitor")
                .withAmountOfRetries(10);
        return jobRequestScheduler.create(job);
    }

    @Chaos
    public JobId schedule(MovieOutboxPurgeJobRequest request) {
        var job = JobBuilder.aJob()
                .scheduleIn(Duration.ofSeconds(1))
                .withName("Movie outbox purge")
                .withJobRequest(request)
                .withLabels("movie", "outbox", "purge")
                .withAmountOfRetries(10);
        return jobRequestScheduler.create(job);
    }

    @Chaos
    public JobId schedule(MovieOutboxRecoverJobRequest request) {
        var job = JobBuilder.aJob()
                .scheduleIn(Duration.ofSeconds(1))
                .withName("Movie outbox recover")
                .withJobRequest(request)
                .withLabels("movie", "outbox", "recover")
                .withAmountOfRetries(10);
        return jobRequestScheduler.create(job);
    }

    @Chaos
    public JobId schedule(MovieHttpDestinationJobRequest request) {
        final var destinationId = request.destinationId();
        final var destination = request.destination();
        var job = JobBuilder.aJob()
                .scheduleIn(Duration.ofSeconds(1))
                .withName("%s: %s".formatted(destinationId, destination))
                .withJobRequest(request)
                .withLabels("movie", "outbox", "destination")
                .withAmountOfRetries(10);
        return jobRequestScheduler.create(job);
    }

    @Chaos
    public JobId schedule(MovieKafkaDestinationJobRequest request) {
        final var destinationId = request.destinationId();
        final var destination = request.destination();
        var job = JobBuilder.aJob()
                .scheduleIn(Duration.ofSeconds(1))
                .withName("%s: %s".formatted(destinationId, destination))
                .withJobRequest(request)
                .withLabels("movie", "outbox", "destination")
                .withAmountOfRetries(10);
        return jobRequestScheduler.create(job);
    }

    @Chaos
    public JobId schedule(MovieOutboxProcessJobRequest request) {
        final var outboxId = request.outboxId();
        var job = JobBuilder.aJob()
                .scheduleIn(Duration.ofSeconds(1))
                .withName(outboxId.toString())
                .withJobRequest(request)
                .withLabels("movie", "outbox", "process")
                .withAmountOfRetries(10);
        return jobRequestScheduler.create(job);
    }

    @Chaos
    public JobId schedule(MovieOutboxDestinationOrchestratorJobRequest request) {
        final var outboxId = request.outboxId();
        var job = JobBuilder.aJob()
                .scheduleIn(Duration.ofSeconds(1))
                .withName(outboxId.toString())
                .withJobRequest(request)
                .withLabels("movie", "outbox", "orchestrator")
                .withAmountOfRetries(10);
        return jobRequestScheduler.create(job);
    }
}
