package com.github.mangila.app.movie.scheduler.outbox.consumer.destination.kafka;

import org.jobrunr.jobs.lambdas.JobRequestHandler;

public class MovieKafkaDestinationJobHandler implements JobRequestHandler<MovieKafkaDestinationJobRequest> {

    @Override
    public void run(MovieKafkaDestinationJobRequest jobRequest) throws Exception {

    }
}
