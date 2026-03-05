package com.github.mangila.app.movie.persistance.outbox.version;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class MovieOutboxVersionJdbcRepository {

	private final JdbcClient jdbcClient;

	public MovieOutboxVersionJdbcRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	public void updateVersion(UUID uuid, Integer version) {
		jdbcClient.sql("""
				UPDATE movie_outbox_version
				SET current_version = :version,
				    updated_at = transaction_timestamp(),
				    modified_by = 'system',
				    audit_version = audit_version + 1
				WHERE aggregate_id = :uuid
				""").param("uuid", uuid).param("version", version).update();
	}

}
