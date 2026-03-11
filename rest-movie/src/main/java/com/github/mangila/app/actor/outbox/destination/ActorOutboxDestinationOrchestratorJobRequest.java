package com.github.mangila.app.actor.outbox.destination;

import org.jobrunr.jobs.lambdas.JobRequest;

import java.util.UUID;

public record ActorOutboxDestinationOrchestratorJobRequest(UUID outboxId) implements JobRequest {

    @Override
    public Class<ActorOutboxDestinationOrchestratorJobHandler> getJobRequestHandler() {
        return ActorOutboxDestinationOrchestratorJobHandler.class;
    }
}
