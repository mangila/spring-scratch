package com.github.mangila.app.movie.persistance.outbox.version;

import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MovieOutboxVersionJpaRepository extends BaseJpaRepository<MovieOutboxVersionEntity, UUID> {

	@Modifying
	@Query("""
			UPDATE movie_outbox_version o
			SET o.currentVersion = o.currentVersion + 1,
			    o.updatedAt = CURRENT_TIMESTAMP
			WHERE o.aggregateId = :aggregateId
			""")
	void increment(UUID aggregateId);

}
