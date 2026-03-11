package com.github.mangila.app.director.outbox.recover;

import org.jobrunr.jobs.lambdas.JobRequest;

public record DirectorOutboxRecoverJobRequest(int limit) implements JobRequest {

    @Override
    public Class<DirectorOutboxRecoverJobHandler> getJobRequestHandler() {
        return DirectorOutboxRecoverJobHandler.class;
    }
}
