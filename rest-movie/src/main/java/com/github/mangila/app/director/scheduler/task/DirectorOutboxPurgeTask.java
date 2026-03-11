package com.github.mangila.app.director.scheduler.task;

import com.github.mangila.app.director.outbox.DirectorOutboxScheduler;
import com.github.mangila.app.director.outbox.purge.DirectorOutboxPurgeJobRequest;
import com.github.mangila.app.director.properties.DirectorOutboxPurgeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DirectorOutboxPurgeTask implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(DirectorOutboxPurgeTask.class);

    private final DirectorOutboxPurgeProperties properties;
    private final DirectorOutboxScheduler directorOutboxScheduler;

    public DirectorOutboxPurgeTask(DirectorOutboxPurgeProperties properties,
                                DirectorOutboxScheduler directorOutboxScheduler) {
        this.properties = properties;
        this.directorOutboxScheduler = directorOutboxScheduler;
    }


    @Override
    public void run() {
        final var limit = properties.getLimit();
        final var request = new DirectorOutboxPurgeJobRequest(limit);
        // TODO: check SUCCESS outbox exist b4 schedule
        var jobId = directorOutboxScheduler.schedule(request);
        log.info("Scheduled purge job with id: {}", jobId);
    }
}
