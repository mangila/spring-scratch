package com.github.mangila.app.director.outbox.monitor;

import org.jobrunr.jobs.lambdas.JobRequest;

public record DirectorOutboxMonitorJobRequest(int limit) implements JobRequest {

    @Override
    public Class<DirectorOutboxMonitorJobHandler> getJobRequestHandler() {
        return DirectorOutboxMonitorJobHandler.class;
    }
}
