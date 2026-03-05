package com.github.mangila.app.movie.persistance.outbox.version;

import com.github.mangila.app.shared.persistence.base.projection.OutboxVersionProjection;
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MovieOutboxVersionJpaRepository extends BaseJpaRepository<MovieOutboxVersionEntity, Integer> {

	@Lock(value = LockModeType.PESSIMISTIC_WRITE)
	@QueryHints({ @QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000") })
	@Query("""
			SELECT v.aggregateId, v.currentVersion FROM movie_outbox_version v
			WHERE v.aggregateId = :id
			""")
	Optional<OutboxVersionProjection> findByIdWithXLock(@Param("id") UUID id);

	@Modifying
	@Query("""
			UPDATE movie_outbox_version v
			SET v.currentVersion = v.currentVersion + 1
			WHERE v.aggregateId = :id
			""")
	int incrementVersionByAggregateId(@Param("id") UUID id);

}
