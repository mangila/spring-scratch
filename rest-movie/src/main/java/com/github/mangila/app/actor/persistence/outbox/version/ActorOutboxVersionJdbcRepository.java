package com.github.mangila.app.actor.persistence.outbox.version;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ActorOutboxVersionJdbcRepository {

	private final JdbcClient jdbcClient;

	public ActorOutboxVersionJdbcRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

}
