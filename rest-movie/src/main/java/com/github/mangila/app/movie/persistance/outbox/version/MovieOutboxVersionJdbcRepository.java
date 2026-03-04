package com.github.mangila.app.movie.persistance.outbox.version;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class MovieOutboxVersionJdbcRepository {

	private final JdbcClient jdbcClient;

	public MovieOutboxVersionJdbcRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

}
