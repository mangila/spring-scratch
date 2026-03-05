package com.github.mangila.app.movie.scheduler.outbox.process;

import com.github.mangila.app.shared.persistence.base.projection.OutboxProjection;
import org.jobrunr.jobs.lambdas.JobRequest;

public record MovieOutboxProcessJobRequest(OutboxProjection outbox) implements JobRequest {

    @Override
    public Class<MovieOutboxProcessJobHandler> getJobRequestHandler() {
        return MovieOutboxProcessJobHandler.class;
    }
}
