package com.github.mangila.app.movie.persistance.outbox;

import com.github.mangila.app.shared.persistence.base.projection.OutboxProjection;
import jakarta.validation.constraints.Positive;
import org.intellij.lang.annotations.Language;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Repository
public class MovieOutboxJdbcRepository {

	private final JdbcClient jdbcClient;

	public MovieOutboxJdbcRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	public List<OutboxProjection> claimOutboxPending(@Positive int limit) {
		@Language("PostgreSQL")
		String sql = """
				WITH claim AS (
					SELECT id
					FROM movie_outbox
					WHERE status = 'PENDING'
					ORDER BY created_at
					LIMIT :limit
					FOR UPDATE SKIP LOCKED
				)
				UPDATE movie_outbox
				SET status = 'PROCESSING',
					updated_at = transaction_timestamp(),
					audit_version = audit_version + 1,
					modified_by = 'system'
				FROM claim
				WHERE movie_outbox.id = claim.id
				RETURNING movie_outbox.id, movie_outbox.history_id, movie_outbox.aggregate_id,
						  movie_outbox.aggregate_version, movie_outbox.status
				""";

		return jdbcClient.sql(sql).param("limit", limit).query(OutboxProjection.class).list();
	}

	public Stream<UUID> streamOutboxIdsReadOnly(int limit) {
		@Language("PostgreSQL")
		String sql = """
				SELECT id
				FROM movie_outbox
				WHERE status = 'PROCESSING'
				LIMIT :limit
				FOR UPDATE SKIP LOCKED
				""";

		return jdbcClient.sql(sql).withFetchSize(256).param("limit", limit).query(UUID.class).stream();
	}

}
