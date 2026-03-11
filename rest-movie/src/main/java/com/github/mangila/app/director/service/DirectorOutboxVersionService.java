package com.github.mangila.app.director.service;

import com.github.mangila.app.director.persistance.outbox.version.DirectorOutboxVersionJdbcRepository;
import com.github.mangila.app.director.persistance.outbox.version.DirectorOutboxVersionJpaRepository;
import com.github.mangila.app.shared.chaos.Chaos;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DirectorOutboxVersionService {

    private final DirectorOutboxVersionJdbcRepository jdbc;

    private final DirectorOutboxVersionJpaRepository jpa;

    public DirectorOutboxVersionService(DirectorOutboxVersionJdbcRepository directorOutboxVersionJdbcRepository,
                                        DirectorOutboxVersionJpaRepository directorOutboxVersionJpaRepository) {
        this.jdbc = directorOutboxVersionJdbcRepository;
        this.jpa = directorOutboxVersionJpaRepository;
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
