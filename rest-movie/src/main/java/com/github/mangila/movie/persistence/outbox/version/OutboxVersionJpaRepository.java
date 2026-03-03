package com.github.mangila.movie.persistence.outbox.version;

import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OutboxVersionJpaRepository extends BaseJpaRepository<OutboxVersionEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT e from outbox_version e
            WHERE e.aggregateId = :aggregateId
            """)
    Optional<OutboxVersionEntity> findByAggregateIdLocked(@Param("aggregateId") UUID aggregateId);

    @Modifying(
            clearAutomatically = true,
            flushAutomatically = true)
    @Query("""
            UPDATE outbox_version e
            SET e.currentVersion = :nextVersion
            WHERE e.aggregateId = :aggregateId
            """)
    void updateVersion(@Param("aggregateId") UUID aggregateId, @Param("nextVersion") int nextVersion);
}
