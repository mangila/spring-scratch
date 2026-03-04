package com.github.mangila.app.actor.persistence.history;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ActorHistoryJdbcRepository {

	private final JdbcClient jdbcClient;

	public ActorHistoryJdbcRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

}
