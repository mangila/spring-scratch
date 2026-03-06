package com.github.mangila.app.movie.persistance.outbox.version;

import org.intellij.lang.annotations.Language;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class MovieOutboxVersionJdbcRepository {

    private final JdbcClient jdbcClient;

    public MovieOutboxVersionJdbcRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public boolean canProcess(UUID aggregateId, int version) {
        @Language("PostgreSQL") final String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM movie_outbox_version
                    WHERE aggregate_id = :aggregateId
                        AND current_version = :version
                )
                """;
        Object ok = jdbcClient.sql(sql)
                .param("aggregateId", aggregateId)
                .param("version", version)
                .query()
                .singleValue();
        return Boolean.TRUE.equals(ok);
    }
}
