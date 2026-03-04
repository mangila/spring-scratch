package com.github.mangila.app.movie.persistance.outbox;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class MovieOutboxJdbcRepository {

	private final JdbcClient jdbcClient;

	public MovieOutboxJdbcRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

}
