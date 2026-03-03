package com.github.mangila.movie.scheduler.outbox.produce.destination;

import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.springframework.stereotype.Component;

@Component
public class ProduceSqsJobHandler implements JobRequestHandler<ProduceSqsJobRequest> {
    @Override
    public void run(ProduceSqsJobRequest jobRequest) throws Exception {

    }
}
