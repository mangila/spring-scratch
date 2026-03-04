package com.github.mangila.app.movie.scheduler.outbox.relay;

import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.springframework.stereotype.Component;

@Component
public class MovieOutboxRelayJobHandler implements JobRequestHandler<MovieOutboxRelayJobRequest> {

    @Override
    public void run(MovieOutboxRelayJobRequest jobRequest) throws Exception {

    }
}
