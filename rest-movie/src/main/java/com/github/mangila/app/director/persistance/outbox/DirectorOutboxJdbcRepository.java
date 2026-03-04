package com.github.mangila.app.director.persistance.outbox;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class DirectorOutboxJdbcRepository {

	private final JdbcClient jdbcClient;

	public DirectorOutboxJdbcRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

}
