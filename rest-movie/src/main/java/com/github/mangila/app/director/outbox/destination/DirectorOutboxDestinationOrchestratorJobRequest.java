package com.github.mangila.app.director.outbox.destination;

import org.jobrunr.jobs.lambdas.JobRequest;

import java.util.UUID;

public record DirectorOutboxDestinationOrchestratorJobRequest(UUID outboxId) implements JobRequest {

    @Override
    public Class<DirectorOutboxDestinationOrchestratorJobHandler> getJobRequestHandler() {
        return DirectorOutboxDestinationOrchestratorJobHandler.class;
    }
}
