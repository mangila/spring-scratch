package com.github.mangila.app.movie.scheduler.task;

import com.github.mangila.app.movie.outbox.MovieOutboxScheduler;
import com.github.mangila.app.movie.outbox.relay.MovieOutboxRelayJobRequest;
import com.github.mangila.app.movie.properties.MovieOutboxRelayProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MovieOutboxRelayTask implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(MovieOutboxRelayTask.class);
    private final MovieOutboxRelayProperties properties;
    private final MovieOutboxScheduler movieOutboxScheduler;

    public MovieOutboxRelayTask(MovieOutboxRelayProperties properties,
                                MovieOutboxScheduler movieOutboxScheduler) {
        this.properties = properties;
        this.movieOutboxScheduler = movieOutboxScheduler;
    }

    @Override
    public void run() {
        final var limit = properties.getLimit();
        final var request = new MovieOutboxRelayJobRequest(limit);
        // TODO: check db exist b4 schedule
        final var jobId = movieOutboxScheduler.schedule(request);
        log.info("Scheduled relay job with id: {}", jobId);
    }
}
