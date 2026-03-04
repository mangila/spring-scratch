package com.github.mangila.app.movie.scheduler.outbox.producer;

import org.jobrunr.jobs.lambdas.JobRequest;

public class MovieOutboxProduceJobRequest implements JobRequest {

    @Override
    public Class<MovieOutboxProduceJobHandler> getJobRequestHandler() {
        return MovieOutboxProduceJobHandler.class;
    }
}
