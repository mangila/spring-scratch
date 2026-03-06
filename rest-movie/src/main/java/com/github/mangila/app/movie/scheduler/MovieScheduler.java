package com.github.mangila.app.movie.scheduler;

import com.github.mangila.app.movie.properties.MovieProperties;
import com.github.mangila.app.movie.scheduler.outbox.monitor.MovieOutboxMonitorJobRequest;
import com.github.mangila.app.movie.scheduler.outbox.destination.MovieOutboxDestinationOrchestratorJobRequest;
import com.github.mangila.app.movie.scheduler.outbox.destination.http.MovieHttpDestinationJobRequest;
import com.github.mangila.app.movie.scheduler.outbox.destination.kafka.MovieKafkaDestinationJobRequest;
import com.github.mangila.app.movie.scheduler.outbox.process.MovieOutboxProcessJobRequest;
import com.github.mangila.app.movie.scheduler.outbox.purge.MovieOutboxPurgeJobRequest;
import com.github.mangila.app.movie.scheduler.outbox.relay.MovieOutboxRelayJobRequest;
import com.github.mangila.app.shared.chaos.Chaos;
import org.intellij.lang.annotations.Language;
import org.jobrunr.jobs.JobId;
import org.jobrunr.scheduling.JobBuilder;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.jobrunr.scheduling.RecurringJobBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class MovieScheduler implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MovieScheduler.class);

    private final JobRequestScheduler jobRequestScheduler;

    private final MovieProperties movieProperties;

    public MovieScheduler(JobRequestScheduler jobRequestScheduler, MovieProperties movieProperties) {
        this.jobRequestScheduler = jobRequestScheduler;
        this.movieProperties = movieProperties;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        final var outbox = movieProperties.getOutbox();
        if (outbox.isEnabled()) {
            var relayJobRequest = new MovieOutboxRelayJobRequest(outbox.getLimit());
            var id = schedule(outbox.getCron(), relayJobRequest);
            log.info("Movie outbox produce relay recurring job scheduled: {}", id);
            var monitorRequest = new MovieOutboxMonitorJobRequest(outbox.getLimit());
            id = schedule(outbox.getCron(), monitorRequest);
            log.info("Movie outbox destination monitor recurring job scheduled: {}", id);
            var purgeRequest = new MovieOutboxPurgeJobRequest(outbox.getLimit());
            id = schedule(outbox.getCron(), purgeRequest);
            log.info("Movie outbox purge recurring job scheduled: {}", id);
        }
    }

    public String schedule(@Language("CronExp") String cron, MovieOutboxRelayJobRequest request) {
        var job = RecurringJobBuilder.aRecurringJob()
                .withCron(cron)
                .withName("Movie outbox relay")
                .withJobRequest(request)
                .withLabels("movie", "outbox", "relay")
                .withAmountOfRetries(10);
        return jobRequestScheduler.createRecurrently(job);
    }

    public String schedule(@Language("CronExp") String cron, MovieOutboxMonitorJobRequest request) {
        var job = RecurringJobBuilder.aRecurringJob()
                .withCron(cron)
                .withName("Movie outbox monitor")
                .withJobRequest(request)
                .withLabels("movie", "outbox", "monitor")
                .withAmountOfRetries(10);
        return jobRequestScheduler.createRecurrently(job);
    }

    public String schedule(@Language("CronExp") String cron, MovieOutboxPurgeJobRequest request) {
        var job = RecurringJobBuilder.aRecurringJob()
                .withCron(cron)
                .withName("Movie outbox purge")
                .withJobRequest(request)
                .withLabels("movie", "outbox", "purge")
                .withAmountOfRetries(10);
        return jobRequestScheduler.createRecurrently(job);
    }

    @Chaos
    public JobId schedule(MovieHttpDestinationJobRequest request) {
        final var destinationId = request.destinationId();
        final var destination = request.destination();
        var job = JobBuilder.aJob()
                .scheduleIn(Duration.ofSeconds(1))
                .withName("Movie %s destination: %s".formatted(destination, destinationId))
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
                .withName("Movie %s destination: %s".formatted(destination, destinationId))
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
                .withName("Movie outbox process: %s".formatted(outboxId))
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
                .withName("Movie outbox destination orchestrator: %s".formatted(outboxId))
                .withJobRequest(request)
                .withLabels("movie", "outbox", "orchestrator")
                .withAmountOfRetries(10);
        return jobRequestScheduler.create(job);
    }
}
