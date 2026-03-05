package com.github.mangila.app.movie.persistance.outbox;

import com.github.mangila.app.shared.persistence.base.projection.OutboxProjection;
import com.github.mangila.app.shared.persistence.type.Status;
import jakarta.validation.constraints.Positive;
import org.intellij.lang.annotations.Language;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MovieOutboxJdbcRepository {

    private final JdbcClient jdbcClient;

    public MovieOutboxJdbcRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<OutboxProjection> claimBatch(@Positive int limit) {
        @Language("PostgreSQL")
        String sql = """
                WITH claim_batch AS (
                	SELECT id
                	FROM movie_outbox
                	WHERE status = 'PENDING'
                	ORDER BY created_at
                	LIMIT :limit
                	FOR UPDATE SKIP LOCKED
                )
                UPDATE movie_outbox
                SET status = 'CLAIMED',
                    updated_at = transaction_timestamp()
                FROM claim_batch
                WHERE movie_outbox.id = claim_batch.id
                RETURNING movie_outbox.id, movie_outbox.history_id,
                          movie_outbox.aggregate_id, movie_outbox.aggregate_version,
                          movie_outbox.status
                """;

        return jdbcClient.sql(sql).param("limit", limit).query(OutboxProjection.class).list();
    }

    public List<OutboxProjection> findAllByStatusSkipLocked(Status status, int limit) {
        @Language("PostgreSQL")
        String sql = """
                SELECT id, history_id, aggregate_id, aggregate_version, status
                FROM movie_outbox
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
