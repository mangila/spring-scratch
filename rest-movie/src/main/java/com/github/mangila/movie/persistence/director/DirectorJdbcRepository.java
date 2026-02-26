package com.github.mangila.movie.persistence.director;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class DirectorJdbcRepository {

    private final JdbcClient jdbcClient;

    public DirectorJdbcRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }
}
