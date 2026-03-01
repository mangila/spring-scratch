package com.github.mangila.movie.persistence.director.history;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class DirectorHistoryJdbcRepository {

	private final JdbcClient jdbcClient;

	public DirectorHistoryJdbcRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

}
