package com.github.mangila.app.movie.persistance.history;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class MovieHistoryJdbcRepository {

	private final JdbcClient jdbcClient;

	public MovieHistoryJdbcRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

}
