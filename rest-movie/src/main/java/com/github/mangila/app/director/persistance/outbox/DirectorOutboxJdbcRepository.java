package com.github.mangila.app.director.persistance.outbox;

import com.github.mangila.app.shared.persistence.base.projection.OutboxProjection;
import com.github.mangila.app.shared.persistence.type.Status;
import jakarta.validation.constraints.Positive;
import org.intellij.lang.annotations.Language;
import org.jobrunr.server.runner.ThreadLocalJobContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class DirectorOutboxJdbcRepository {

	private final JdbcClient jdbcClient;

	public DirectorOutboxJdbcRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	public List<UUID> claimBatch(Status from, Status to, @Positive int limit) {
		final var context = ThreadLocalJobContext.getJobContext();
		@Language("PostgreSQL")
		String sql = """
				WITH claim_batch AS (
					SELECT id
					FROM director_outbox
					WHERE status = CAST(:from AS status)
					ORDER BY created_at
					LIMIT :limit
					FOR UPDATE SKIP LOCKED
				)
				UPDATE director_outbox
				SET status = CAST(:to AS status),
					updated_at = transaction_timestamp(),
					modified_by = :modifiedBy
				FROM claim_batch
				WHERE director_outbox.id = claim_batch.id
				RETURNING director_outbox.id
				""";

		return jdbcClient.sql(sql)
				.param("to", to.toString())
				.param("from", from.toString())
				.param("modifiedBy", context.getJobId())
				.param("limit", limit)
				.query(UUID.class)
				.list();
	}

	public List<OutboxProjection> findAllByStatusSkipLocked(Status status, int limit) {
		@Language("PostgreSQL")
		String sql = """
				SELECT id, history_id, aggregate_id, aggregate_version, status
				FROM director_outbox
				WHERE status = CAST(:status AS status)
				ORDER BY created_at
				LIMIT :limit
				FOR UPDATE SKIP LOCKED
				""";
		return jdbcClient.sql(sql)
				.withFetchSize(256)
				.param("status", status.toString())
				.param("limit", limit)
				.query(OutboxProjection.class)
				.list();
	}

}
