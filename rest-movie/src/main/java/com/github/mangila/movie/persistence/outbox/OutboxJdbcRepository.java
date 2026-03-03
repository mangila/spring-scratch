package com.github.mangila.movie.persistence.outbox;

import com.github.mangila.movie.persistence.outbox.projection.OutboxProjection;
import org.intellij.lang.annotations.Language;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class OutboxJdbcRepository {

    private final JdbcClient jdbcClient;

    public OutboxJdbcRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public List<OutboxProjection> claimPending(int limit) {
        @Language("PostgreSQL") final String sql = """
                WITH claimed_rows AS (
                    SELECT id
                    FROM outbox
                    WHERE status = 'PENDING'
                    ORDER BY created_at, version
                    LIMIT :limit
                    FOR UPDATE SKIP LOCKED
                )
                UPDATE outbox
                SET status = 'PROCESSING'
                FROM claimed_rows
                WHERE outbox.id = claimed_rows.id
                RETURNING outbox.id, outbox.history_id, outbox.aggregate_id, outbox.status, outbox.version
                """;
        return jdbcClient.sql(sql)
                .param("limit", limit)
                .query(OutboxProjection.class)
                .list();
    }

}
