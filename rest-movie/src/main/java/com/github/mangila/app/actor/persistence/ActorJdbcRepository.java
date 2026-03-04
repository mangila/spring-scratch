package com.github.mangila.app.actor.persistence;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ActorJdbcRepository {

	private final JdbcClient jdbcClient;

	public ActorJdbcRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

}
