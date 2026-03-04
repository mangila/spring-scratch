package com.github.mangila.app.movie.scheduler.outbox.producer;

import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.springframework.stereotype.Component;

@Component
public class MovieOutboxProduceJobHandler implements JobRequestHandler<MovieOutboxProduceJobRequest> {

    @Override
    public void run(MovieOutboxProduceJobRequest jobRequest) throws Exception {

    }
}
