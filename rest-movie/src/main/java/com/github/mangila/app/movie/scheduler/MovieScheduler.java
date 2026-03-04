package com.github.mangila.app.movie.scheduler;

import com.github.mangila.app.movie.properties.MovieProperties;
import com.github.mangila.app.movie.scheduler.outbox.consumer.MovieOutboxConsumeRelayJobRequest;
import com.github.mangila.app.movie.scheduler.outbox.consumer.destination.http.MovieHttpDestinationJobRequest;
import com.github.mangila.app.movie.scheduler.outbox.consumer.destination.kafka.MovieKafkaDestinationJobRequest;
import com.github.mangila.app.movie.scheduler.outbox.producer.MovieOutboxProduceJobRequest;
import com.github.mangila.app.movie.scheduler.outbox.producer.MovieOutboxProduceRelayJobRequest;
import org.intellij.lang.annotations.Language;
import org.jobrunr.jobs.JobId;
import org.jobrunr.scheduling.JobBuilder;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.jobrunr.scheduling.RecurringJobBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class MovieScheduler {

    private static final Logger log = LoggerFactory.getLogger(MovieScheduler.class);

    private final JobRequestScheduler jobRequestScheduler;

    private final MovieProperties movieProperties;

    public MovieScheduler(JobRequestScheduler jobRequestScheduler, MovieProperties movieProperties) {
        this.jobRequestScheduler = jobRequestScheduler;
        this.movieProperties = movieProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        final var outbox = movieProperties.getOutbox();
        if (outbox.isEnabled()) {
            var produceRelayJobRequest = new MovieOutboxProduceRelayJobRequest(outbox.getLimit());
            var id = schedule(outbox.getCron(), produceRelayJobRequest);
            log.info("Movie outbox produce relay reccuring job scheduled: {}", id);
            var consumeRelayJobRequest = new MovieOutboxConsumeRelayJobRequest(outbox.getLimit());
            id = schedule(outbox.getCron(), consumeRelayJobRequest);
            log.info("Movie outbox consume relay reccuring job scheduled: {}", id);
        }
    }

    public String schedule(@Language("CronExp") String cron, MovieOutboxProduceRelayJobRequest request) {
        var job = RecurringJobBuilder.aRecurringJob()
                .withCron(cron)
                .withName("Movie outbox produce relay")
                .withJobRequest(request)
                .withLabels("movie", "outbox", "produce")
                .withAmountOfRetries(10);
        return jobRequestScheduler.createRecurrently(job);
    }

    public String schedule(@Language("CronExp") String cron, MovieOutboxConsumeRelayJobRequest request) {
        var job = RecurringJobBuilder.aRecurringJob()
                .withCron(cron)
                .withName("Movie outbox consume relay")
                .withJobRequest(request)
                .withLabels("movie", "outbox", "consume")
                .withAmountOfRetries(10);
        return jobRequestScheduler.createRecurrently(job);
    }

    public JobId schedule(MovieOutboxProduceJobRequest request) {
        var job = JobBuilder.aJob()
                .scheduleIn(Duration.ofSeconds(1))
                .withName("Movie outbox produce: %s".formatted(request.outbox().id()))
                .withJobRequest(request)
                .withLabels("movie", "outbox", "produce")
                .withAmountOfRetries(10);
        return jobRequestScheduler.create(job);
    }

    public JobId schedule(MovieHttpDestinationJobRequest request) {
        var job = JobBuilder.aJob()
                .scheduleIn(Duration.ofSeconds(1))
                .withName("Movie HTTP destination")
                .withJobRequest(request)
                .withLabels("movie", "outbox", "destination")
                .withAmountOfRetries(10);
        return jobRequestScheduler.create(job);
    }

    public JobId schedule(MovieKafkaDestinationJobRequest request) {
        var job = JobBuilder.aJob()
                .scheduleIn(Duration.ofSeconds(1))
                .withName("Movie KAFKA destination")
                .withJobRequest(request)
                .withLabels("movie", "outbox", "destination")
                .withAmountOfRetries(10);
        return jobRequestScheduler.create(job);
    }
}
