package com.github.mangila.app.movie.scheduler.outbox.producer;

import com.github.mangila.app.movie.persistance.projection.MovieOutboxProjection;
import org.jobrunr.jobs.lambdas.JobRequest;

public record MovieOutboxProduceJobRequest(MovieOutboxProjection outbox) implements JobRequest {

    @Override
    public Class<MovieOutboxProduceJobHandler> getJobRequestHandler() {
        return MovieOutboxProduceJobHandler.class;
    }
}
