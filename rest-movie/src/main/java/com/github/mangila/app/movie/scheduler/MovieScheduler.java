package com.github.mangila.app.movie.scheduler;

import com.github.mangila.app.movie.properties.MovieProperties;
import com.github.mangila.app.movie.scheduler.outbox.producer.MovieOutboxProduceJobRequest;
import com.github.mangila.app.movie.scheduler.outbox.relay.MovieOutboxRelayJobRequest;
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

    public MovieScheduler(JobRequestScheduler jobRequestScheduler,
                          MovieProperties movieProperties) {
        this.jobRequestScheduler = jobRequestScheduler;
        this.movieProperties = movieProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        final var outbox = movieProperties.getOutbox();
        if (outbox.isEnabled()) {
            log.info("Movie outbox enabled");
            var jobRequest = new MovieOutboxRelayJobRequest(outbox.getLimit());
            var id = schedule(outbox.getCron(), jobRequest);
            log.info("Movie outbox relay scheduled with id: {}", id);
        }
    }

    public String schedule(@Language("CronExp") String cron, MovieOutboxRelayJobRequest request) {
        var job = RecurringJobBuilder.aRecurringJob()
                .withCron(cron)
                .withName("Movie outbox relay")
                .withJobRequest(request)
                .withLabels("movie", "outbox")
                .withAmountOfRetries(10);
        return jobRequestScheduler.createRecurrently(job);
    }

    public JobId schedule(MovieOutboxProduceJobRequest request) {
        var job = JobBuilder.aJob()
                .scheduleIn(Duration.ofSeconds(1))
                .withName("Movie outbox produce: %s".formatted(request.outbox().id()))
                .withJobRequest(request)
                .withLabels("movie", "outbox")
                .withAmountOfRetries(10);
        return jobRequestScheduler.create(job);
    }
}
