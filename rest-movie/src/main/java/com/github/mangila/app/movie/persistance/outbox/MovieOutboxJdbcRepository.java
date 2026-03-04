package com.github.mangila.app.movie.persistance.outbox;

import com.github.mangila.app.shared.persistence.base.projection.OutboxProjection;
import jakarta.validation.constraints.Positive;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MovieOutboxJdbcRepository {

	private final JdbcClient jdbcClient;

	public MovieOutboxJdbcRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	public List<OutboxProjection> claimOutboxPending(@Positive int limit) {
		String sql = """
				UPDATE movie_outbox
				SET status = 'PROCESSING',
				    updated_at = transaction_timestamp(),
				    audit_version = audit_version + 1,
				    modified_by = 'system'
				WHERE id IN (
				    SELECT id
				    FROM movie_outbox
				    WHERE status = 'PENDING'
				    ORDER BY created_at
				    LIMIT :limit
				    FOR UPDATE SKIP LOCKED
				)
				RETURNING id, history_id, aggregate_id, aggregate_version, status
				""";

		return jdbcClient.sql(sql).param("limit", limit).query(OutboxProjection.class).list();
	}

}
