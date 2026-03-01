package com.github.mangila.movie.persistence.movie.history;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class MovieHistoryJdbcRepository {

	private final JdbcClient jdbcClient;

	public MovieHistoryJdbcRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

}
