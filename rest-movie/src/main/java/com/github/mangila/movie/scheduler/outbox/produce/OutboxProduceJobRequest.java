package com.github.mangila.movie.scheduler.outbox.produce;

import com.github.mangila.movie.persistence.outbox.projection.OutboxProjection;
import org.jobrunr.jobs.lambdas.JobRequest;

public record OutboxProduceJobRequest(OutboxProjection outbox) implements JobRequest {

    @Override
    public Class<OutboxProduceJobHandler> getJobRequestHandler() {
        return OutboxProduceJobHandler.class;
    }
}
