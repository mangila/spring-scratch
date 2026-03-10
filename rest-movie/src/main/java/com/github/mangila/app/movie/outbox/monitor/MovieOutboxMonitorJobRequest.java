package com.github.mangila.app.movie.outbox.monitor;

import org.jobrunr.jobs.lambdas.JobRequest;

public record MovieOutboxMonitorJobRequest(int limit) implements JobRequest {

    @Override
    public Class<MovieOutboxMonitorJobHandler> getJobRequestHandler() {
        return MovieOutboxMonitorJobHandler.class;
    }
}
