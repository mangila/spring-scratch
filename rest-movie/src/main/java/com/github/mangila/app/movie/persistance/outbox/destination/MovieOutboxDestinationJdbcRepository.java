package com.github.mangila.app.movie.persistance.outbox.destination;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class MovieOutboxDestinationJdbcRepository {

    private final JdbcClient jdbcClient;

    public MovieOutboxDestinationJdbcRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }
}
