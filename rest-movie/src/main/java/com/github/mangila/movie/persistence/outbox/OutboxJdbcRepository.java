package com.github.mangila.movie.persistence.outbox;

import com.github.mangila.movie.persistence.outbox.projection.OutboxProjection;
import org.intellij.lang.annotations.Language;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class OutboxJdbcRepository {

	private final JdbcClient jdbcClient;

	public OutboxJdbcRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	@Transactional
	public List<OutboxProjection> claimProcessing(int limit) {
		@Language("PostgreSQL")
		final String sql = """
				UPDATE outbox
				SET status = 'PROCESSING'
				WHERE id IN (
				    SELECT id
				    FROM outbox
				    WHERE status = 'PENDING'
				    ORDER BY created_at, version
				    LIMIT :limit
				    FOR UPDATE SKIP LOCKED
				)
				RETURNING id, history_id, aggregate_id, status, version;
				""";
		return jdbcClient.sql(sql).param("limit", limit).query(OutboxProjection.class).list();
	}

}
