package com.github.mangila.movie.scheduler.outbox.produce.destination;

import com.github.mangila.movie.persistence.outbox.projection.OutboxProjection;
import org.jobrunr.jobs.lambdas.JobRequest;

public record ProduceKafkaJobRequest(OutboxProjection outbox) implements JobRequest {
    @Override
    public Class<ProduceKafkaJobHandler> getJobRequestHandler() {
        return ProduceKafkaJobHandler.class;
    }
}
