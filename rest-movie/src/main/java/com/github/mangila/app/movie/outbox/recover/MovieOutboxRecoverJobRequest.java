package com.github.mangila.app.movie.outbox.recover;

import org.jobrunr.jobs.lambdas.JobRequest;

public record MovieOutboxRecoverJobRequest(int limit) implements JobRequest {

    @Override
    public Class<MovieOutboxRecoverJobHandler> getJobRequestHandler() {
        return MovieOutboxRecoverJobHandler.class;
    }
}
