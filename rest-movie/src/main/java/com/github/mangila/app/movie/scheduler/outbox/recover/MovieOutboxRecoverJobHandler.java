package com.github.mangila.app.movie.scheduler.outbox.recover;

import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.springframework.stereotype.Component;

@Component
public class MovieOutboxRecoverJobHandler implements JobRequestHandler<MovieOutboxRecoverJobRequest> {

    @Override
    public void run(MovieOutboxRecoverJobRequest jobRequest) throws Exception {

    }
}
