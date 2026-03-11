package com.github.mangila.app.actor.scheduler.task;

import com.github.mangila.app.actor.outbox.ActorOutboxScheduler;
import com.github.mangila.app.actor.outbox.relay.ActorOutboxRelayJobRequest;
import com.github.mangila.app.actor.properties.ActorOutboxRelayProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActorOutboxRelayTask implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ActorOutboxRelayTask.class);

    private final ActorOutboxRelayProperties properties;
    private final ActorOutboxScheduler actorOutboxScheduler;

    public ActorOutboxRelayTask(ActorOutboxRelayProperties properties,
                                ActorOutboxScheduler actorOutboxScheduler) {
        this.properties = properties;
        this.actorOutboxScheduler = actorOutboxScheduler;
    }

    @Override
    public void run() {
        final var limit = properties.getLimit();
        final var request = new ActorOutboxRelayJobRequest(limit);
        // TODO: check PENDING outbox exist b4 schedule
        final var jobId = actorOutboxScheduler.schedule(request);
        log.info("Scheduled relay job with id: {}", jobId);
    }
}
