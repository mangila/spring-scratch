package com.github.mangila.app.movie.scheduler.task;

import com.github.mangila.app.movie.outbox.MovieOutboxScheduler;
import com.github.mangila.app.movie.outbox.purge.MovieOutboxPurgeJobRequest;
import com.github.mangila.app.movie.properties.MovieOutboxPurgeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MovieOutboxPurgeTask implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(MovieOutboxPurgeTask.class);

    private final MovieOutboxPurgeProperties properties;
    private final MovieOutboxScheduler movieOutboxScheduler;

    public MovieOutboxPurgeTask(MovieOutboxPurgeProperties properties,
                                MovieOutboxScheduler movieOutboxScheduler) {
        this.properties = properties;
        this.movieOutboxScheduler = movieOutboxScheduler;
    }


    @Override
    public void run() {
        final var limit = properties.getLimit();
        final var request = new MovieOutboxPurgeJobRequest(limit);
        // TODO: check SUCCESS outbox exist b4 schedule
        var jobId = movieOutboxScheduler.schedule(request);
        log.info("Scheduled purge job with id: {}", jobId);
    }
}
