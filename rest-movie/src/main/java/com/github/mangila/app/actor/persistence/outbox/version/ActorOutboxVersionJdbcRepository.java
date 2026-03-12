package com.github.mangila.app.actor.persistence.outbox.version;

import org.intellij.lang.annotations.Language;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class ActorOutboxVersionJdbcRepository {

	private final JdbcClient jdbcClient;

	public ActorOutboxVersionJdbcRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	public boolean canProcess(UUID aggregateId, int version) {
		@Language("PostgreSQL")
		final String sql = """
				SELECT EXISTS (
					SELECT 1
					FROM actor_outbox_version
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
