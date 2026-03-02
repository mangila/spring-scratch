package com.github.mangila.movie.persistence.outbox.version;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class OutboxVersionJdbcRepository {

	private final JdbcClient jdbcClient;

	public OutboxVersionJdbcRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

}
