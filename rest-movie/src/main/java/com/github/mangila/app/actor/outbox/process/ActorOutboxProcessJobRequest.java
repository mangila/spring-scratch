package com.github.mangila.app.actor.outbox.process;

import org.jobrunr.jobs.lambdas.JobRequest;

import java.util.UUID;

public record ActorOutboxProcessJobRequest(UUID outboxId) implements JobRequest {

    @Override
    public Class<ActorOutboxProcessJobHandler> getJobRequestHandler() {
        return ActorOutboxProcessJobHandler.class;
    }
}
