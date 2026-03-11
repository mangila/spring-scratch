package com.github.mangila.app.movie.service;

import com.github.mangila.app.movie.persistance.outbox.version.MovieOutboxVersionJdbcRepository;
import com.github.mangila.app.movie.persistance.outbox.version.MovieOutboxVersionJpaRepository;
import com.github.mangila.app.shared.chaos.Chaos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class MovieOutboxVersionService {

    private final MovieOutboxVersionJdbcRepository jdbc;

    private final MovieOutboxVersionJpaRepository jpa;

    public MovieOutboxVersionService(MovieOutboxVersionJdbcRepository movieOutboxVersionJdbcRepository,
                                     MovieOutboxVersionJpaRepository movieOutboxVersionJpaRepository) {
        this.jdbc = movieOutboxVersionJdbcRepository;
        this.jpa = movieOutboxVersionJpaRepository;
    }

    @Chaos
    public boolean canProcess(UUID aggregateId, int version) {
        return jdbc.canProcess(aggregateId, version);
    }

    @Chaos
    @Transactional(propagation = Propagation.MANDATORY)
    public void increment(UUID aggregateId) {
        jpa.increment(aggregateId);
    }
}
