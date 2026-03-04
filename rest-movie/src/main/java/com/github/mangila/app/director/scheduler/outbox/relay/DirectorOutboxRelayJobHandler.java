package com.github.mangila.app.director.scheduler.outbox.relay;

import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.springframework.stereotype.Component;

@Component
public class DirectorOutboxRelayJobHandler implements JobRequestHandler<DirectorOutboxRelayJobRequest> {

    @Override
    public void run(DirectorOutboxRelayJobRequest jobRequest) throws Exception {

    }
}
