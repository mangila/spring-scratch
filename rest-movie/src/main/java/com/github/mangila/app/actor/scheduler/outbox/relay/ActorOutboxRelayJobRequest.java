package com.github.mangila.app.actor.scheduler.outbox.relay;

import org.jobrunr.jobs.lambdas.JobRequest;

public record ActorOutboxRelayJobRequest(int limit) implements JobRequest {

    @Override
    public Class<ActorOutboxRelayJobHandler> getJobRequestHandler() {
        return ActorOutboxRelayJobHandler.class;
    }
}
