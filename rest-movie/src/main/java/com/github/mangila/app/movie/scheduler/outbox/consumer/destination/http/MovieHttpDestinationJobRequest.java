package com.github.mangila.app.movie.scheduler.outbox.consumer.destination.http;

import org.jobrunr.jobs.lambdas.JobRequest;

public record MovieHttpDestinationJobRequest(com.fasterxml.jackson.databind.JsonNode payload) implements JobRequest {
    @Override
    public Class<MovieHttpDestinationJobHandler> getJobRequestHandler() {
        return MovieHttpDestinationJobHandler.class;
    }
}
