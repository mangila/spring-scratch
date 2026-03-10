package com.github.mangila.app.movie.scheduler.task;

import com.github.mangila.app.movie.outbox.MovieOutboxScheduler;
import com.github.mangila.app.movie.outbox.monitor.MovieOutboxMonitorJobRequest;
import com.github.mangila.app.movie.properties.MovieOutboxMonitorProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MovieOutboxMonitorTask implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(MovieOutboxMonitorTask.class);
    private final MovieOutboxMonitorProperties properties;
    private final MovieOutboxScheduler movieOutboxScheduler;

    public MovieOutboxMonitorTask(MovieOutboxMonitorProperties properties,
                                  MovieOutboxScheduler movieOutboxScheduler) {
        this.properties = properties;
        this.movieOutboxScheduler = movieOutboxScheduler;
    }

    @Override
    public void run() {
        final var limit = properties.getLimit();
        final var request = new MovieOutboxMonitorJobRequest(limit);

        // TODO: check db exist b4 schedule

        final var jobId = movieOutboxScheduler.schedule(request);
        log.info("Scheduled monitor job with id: {}", jobId);
    }
}
