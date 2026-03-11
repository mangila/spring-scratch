package com.github.mangila.app.actor.service;

import com.github.mangila.app.actor.persistence.outbox.version.ActorOutboxVersionJdbcRepository;
import com.github.mangila.app.actor.persistence.outbox.version.ActorOutboxVersionJpaRepository;
import com.github.mangila.app.shared.chaos.Chaos;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ActorOutboxVersionService {

    private final ActorOutboxVersionJdbcRepository jdbc;

    private final ActorOutboxVersionJpaRepository jpa;

    public ActorOutboxVersionService(ActorOutboxVersionJdbcRepository actorOutboxVersionJdbcRepository,
                                     ActorOutboxVersionJpaRepository actorOutboxVersionJpaRepository) {
        this.jdbc = actorOutboxVersionJdbcRepository;
        this.jpa = actorOutboxVersionJpaRepository;
    }

    @Chaos
    public boolean canProcess(UUID aggregateId, int version) {
        return jdbc.canProcess(aggregateId, version);
    }

    @Chaos
    public void increment(UUID aggregateId) {
        jpa.increment(aggregateId);
    }
}
