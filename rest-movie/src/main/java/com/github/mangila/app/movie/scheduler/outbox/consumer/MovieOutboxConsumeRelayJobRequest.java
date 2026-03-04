package com.github.mangila.app.movie.scheduler.outbox.consumer;

import org.jobrunr.jobs.lambdas.JobRequest;

public record MovieOutboxConsumeRelayJobRequest(int limit) implements JobRequest {

    @Override
    public Class<MovieOutboxConsumeRelayJobHandler> getJobRequestHandler() {
        return MovieOutboxConsumeRelayJobHandler.class;
    }
}
