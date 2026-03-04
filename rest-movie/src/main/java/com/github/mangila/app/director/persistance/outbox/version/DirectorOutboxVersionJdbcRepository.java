package com.github.mangila.app.director.persistance.outbox.version;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class DirectorOutboxVersionJdbcRepository {

	private final JdbcClient jdbcClient;

	public DirectorOutboxVersionJdbcRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

}
