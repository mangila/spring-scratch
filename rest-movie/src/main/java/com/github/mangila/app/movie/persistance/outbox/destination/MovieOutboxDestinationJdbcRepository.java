package com.github.mangila.app.movie.persistance.outbox.destination;

import com.github.mangila.app.shared.persistence.base.projection.OutboxDestinationProjection;
import com.github.mangila.app.shared.persistence.type.Status;
import jakarta.validation.constraints.Positive;
import org.intellij.lang.annotations.Language;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class MovieOutboxDestinationJdbcRepository {

	private final JdbcClient jdbcClient;

	public MovieOutboxDestinationJdbcRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	public List<OutboxDestinationProjection> claimDestinationPending(@Positive int limit) {
		@Language("PostgreSQL")
		String sql = """
				WITH claim AS (
					SELECT id
					FROM movie_outbox_destination
					WHERE status = 'PENDING'
					ORDER BY created_at
					LIMIT :limit
					FOR UPDATE SKIP LOCKED
				)
				UPDATE movie_outbox_destination
				SET status = 'PROCESSING',
					updated_at = transaction_timestamp(),
					audit_version = audit_version + 1,
					modified_by = 'system'
				FROM claim
				WHERE movie_outbox_destination.id = claim.id
				RETURNING movie_outbox_destination.id, movie_outbox_destination.outbox_id,
				    	  movie_outbox_destination.destination,
						  movie_outbox_destination.status
				""";
		return jdbcClient.sql(sql).param("limit", limit).query(OutboxDestinationProjection.class).list();
	}

	public void updateStatus(UUID destinationId, Status status) {
		@Language("PostgreSQL")
		String sql = """
				UPDATE movie_outbox_destination
				SET status = :status,
					updated_at = transaction_timestamp(),
					audit_version = audit_version + 1,
					modified_by = 'system'
				WHERE id = :destinationId
				""";

		jdbcClient.sql(sql).param("destinationId", destinationId).param("status", status.toString()).update();
	}

}
