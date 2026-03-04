package com.github.mangila.app.movie.scheduler.outbox.relay;

import org.jobrunr.jobs.lambdas.JobRequest;

public record MovieOutboxRelayJobRequest(int limit) implements JobRequest {

    @Override
    public Class<MovieOutboxRelayJobHandler> getJobRequestHandler() {
        return MovieOutboxRelayJobHandler.class;
    }
}
