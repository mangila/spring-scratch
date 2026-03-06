package com.github.mangila.app.movie.persistance.outbox.destination;

import com.github.mangila.app.shared.persistence.base.projection.OutboxDestinationProjection;
import com.github.mangila.app.shared.persistence.type.Status;
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

    public List<OutboxDestinationProjection> claimBatch(UUID outboxId, Status from, Status to) {
        @Language("PostgreSQL")
        String sql = """
                WITH claim_batch AS (
                	SELECT id
                	FROM movie_outbox_destination
                	WHERE
                	    outbox_id = :outboxId
                	    AND
                	    status = CAST(:from AS status)
                	ORDER BY created_at
                	FOR UPDATE SKIP LOCKED
                )
                UPDATE movie_outbox_destination
                SET status = CAST(:to AS status),
                    updated_at = transaction_timestamp()
                FROM claim_batch
                WHERE movie_outbox_destination.id = claim_batch.id
                RETURNING movie_outbox_destination.id, movie_outbox_destination.outbox_id,
                    movie_outbox_destination.destination,
                    movie_outbox_destination.status
                """;

        return jdbcClient.sql(sql)
                .param("outboxId", outboxId)
                .param("to", to.toString())
                .param("from", from.toString())
                .query(OutboxDestinationProjection.class)
                .list();
    }
}
