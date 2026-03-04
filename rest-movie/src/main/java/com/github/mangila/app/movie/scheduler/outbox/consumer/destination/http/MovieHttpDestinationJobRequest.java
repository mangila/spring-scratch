package com.github.mangila.app.movie.scheduler.outbox.consumer.destination.http;

import org.jobrunr.jobs.lambdas.JobRequest;

public record MovieHttpDestinationJobRequest() implements JobRequest {
    @Override
    public Class<MovieHttpDestinationJobHandler> getJobRequestHandler() {
        return MovieHttpDestinationJobHandler.class;
    }
}
