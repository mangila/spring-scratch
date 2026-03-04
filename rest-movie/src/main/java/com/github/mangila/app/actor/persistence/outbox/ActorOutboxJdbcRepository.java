package com.github.mangila.app.actor.persistence.outbox;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ActorOutboxJdbcRepository {

	private final JdbcClient jdbcClient;

	public ActorOutboxJdbcRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

}
