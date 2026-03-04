package com.github.mangila.app.movie.scheduler.outbox.producer;

import com.github.mangila.app.shared.persistence.base.projection.OutboxProjection;
import org.jobrunr.jobs.lambdas.JobRequest;

public record MovieOutboxProduceJobRequest(OutboxProjection outbox) implements JobRequest {

    @Override
    public Class<MovieOutboxProduceJobHandler> getJobRequestHandler() {
        return MovieOutboxProduceJobHandler.class;
    }
}
