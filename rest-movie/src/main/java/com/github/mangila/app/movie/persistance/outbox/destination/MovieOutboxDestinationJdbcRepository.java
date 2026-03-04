package com.github.mangila.app.movie.persistance.outbox.destination;

import com.github.mangila.app.shared.persistence.base.projection.OutboxDestinationProjection;
import jakarta.validation.constraints.Positive;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MovieOutboxDestinationJdbcRepository {

    private final JdbcClient jdbcClient;

    public MovieOutboxDestinationJdbcRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<OutboxDestinationProjection> claimDestinationPending(@Positive int limit) {
        String sql = """
                UPDATE movie_outbox_destination
                SET status = 'PROCESSING',
                    updated_at = transaction_timestamp(),
                    audit_version = audit_version + 1,
                    modified_by = 'system'
                WHERE id IN (
                    SELECT id
                    FROM movie_outbox_destination
                    WHERE status = 'PENDING'
                    ORDER BY created_at
                    LIMIT :limit
                    FOR UPDATE SKIP LOCKED
                )
                RETURNING id, outbox_id, destination, status
                """;

        return jdbcClient.sql(sql).param("limit", limit).query(OutboxDestinationProjection.class).list();
    }
}
