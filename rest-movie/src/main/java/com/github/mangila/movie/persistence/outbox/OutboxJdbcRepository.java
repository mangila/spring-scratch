package com.github.mangila.movie.persistence.outbox;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class OutboxJdbcRepository {

	private final JdbcClient jdbcClient;

	public OutboxJdbcRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

}
