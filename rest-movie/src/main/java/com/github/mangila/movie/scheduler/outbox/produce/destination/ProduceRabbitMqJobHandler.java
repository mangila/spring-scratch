package com.github.mangila.movie.scheduler.outbox.produce.destination;

import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.springframework.stereotype.Component;

@Component
public class ProduceRabbitMqJobHandler implements JobRequestHandler<ProduceRabbitMqJobRequest> {
    @Override
    public void run(ProduceRabbitMqJobRequest jobRequest) throws Exception {

    }
}
