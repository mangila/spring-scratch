package com.github.mangila.app.movie.scheduler.outbox.recover;

import org.jobrunr.jobs.lambdas.JobRequest;

public record MovieOutboxRecoverJobRequest() implements JobRequest {

    @Override
    public Class<MovieOutboxRecoverJobHandler> getJobRequestHandler() {
        return MovieOutboxRecoverJobHandler.class;
    }
}
