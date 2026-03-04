package com.github.mangila.app.actor.scheduler.outbox.producer;

import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.springframework.stereotype.Component;

@Component
public class ActorOutboxProduceJobHandler implements JobRequestHandler<ActorOutboxProduceJobRequest> {

    @Override
    public void run(ActorOutboxProduceJobRequest jobRequest) throws Exception {

    }
}
