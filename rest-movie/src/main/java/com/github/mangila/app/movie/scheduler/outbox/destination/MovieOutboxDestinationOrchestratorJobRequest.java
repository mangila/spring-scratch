package com.github.mangila.app.movie.scheduler.outbox.destination;

import org.jobrunr.jobs.lambdas.JobRequest;

import java.util.UUID;

public record MovieOutboxDestinationOrchestratorJobRequest(UUID outboxId) implements JobRequest {

    @Override
    public Class<MovieOutboxDestinationOrchestratorJobHandler> getJobRequestHandler() {
        return MovieOutboxDestinationOrchestratorJobHandler.class;
    }
}
