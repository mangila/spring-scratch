package com.github.mangila.movie.scheduler.outbox.produce.destination;

import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.springframework.stereotype.Component;

@Component
public class ProduceKafkaJobHandler implements JobRequestHandler<ProduceKafkaJobRequest> {
    @Override
    public void run(ProduceKafkaJobRequest jobRequest) throws Exception {

    }
}
