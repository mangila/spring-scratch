package com.github.mangila.app.movie.scheduler.outbox.consumer.destination.kafka;

import org.jobrunr.jobs.lambdas.JobRequest;

public record MovieKafkaDestinationJobRequest() implements JobRequest {
    @Override
    public Class<MovieKafkaDestinationJobHandler> getJobRequestHandler() {
        return MovieKafkaDestinationJobHandler.class;
    }
}
