package com.github.mangila.movie.persistence;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class MovieJdbcRepository {

    private final JdbcClient jdbcClient;

    public MovieJdbcRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }
}
