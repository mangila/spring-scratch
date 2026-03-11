package com.github.mangila.app.actor.scheduler.task;

import com.github.mangila.app.actor.outbox.ActorOutboxScheduler;
import com.github.mangila.app.actor.outbox.purge.ActorOutboxPurgeJobRequest;
import com.github.mangila.app.actor.properties.ActorOutboxPurgeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActorOutboxPurgeTask implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ActorOutboxPurgeTask.class);

    private final ActorOutboxPurgeProperties properties;
    private final ActorOutboxScheduler actorOutboxScheduler;

    public ActorOutboxPurgeTask(ActorOutboxPurgeProperties properties,
                                ActorOutboxScheduler actorOutboxScheduler) {
        this.properties = properties;
        this.actorOutboxScheduler = actorOutboxScheduler;
    }


    @Override
    public void run() {
        final var limit = properties.getLimit();
        final var request = new ActorOutboxPurgeJobRequest(limit);
        // TODO: check SUCCESS outbox exist b4 schedule
        var jobId = actorOutboxScheduler.schedule(request);
        log.info("Scheduled purge job with id: {}", jobId);
    }
}
