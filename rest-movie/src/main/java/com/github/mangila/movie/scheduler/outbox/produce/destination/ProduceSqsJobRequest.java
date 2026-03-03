package com.github.mangila.movie.scheduler.outbox.produce.destination;

import com.github.mangila.movie.persistence.outbox.projection.OutboxProjection;
import org.jobrunr.jobs.lambdas.JobRequest;

public record ProduceSqsJobRequest(OutboxProjection outbox) implements JobRequest {
    @Override
    public Class<ProduceSqsJobHandler> getJobRequestHandler() {
        return ProduceSqsJobHandler.class;
    }
}
