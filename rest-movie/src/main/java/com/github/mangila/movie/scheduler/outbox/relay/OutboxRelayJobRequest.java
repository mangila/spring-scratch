package com.github.mangila.movie.scheduler.outbox.relay;

import org.jobrunr.jobs.lambdas.JobRequest;

public record OutboxRelayJobRequest(int limit) implements JobRequest {

    @Override
    public Class<OutboxRelayJobHandler> getJobRequestHandler() {
        return OutboxRelayJobHandler.class;
    }

}
