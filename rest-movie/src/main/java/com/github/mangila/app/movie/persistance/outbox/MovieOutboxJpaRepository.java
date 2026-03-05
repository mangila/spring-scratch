package com.github.mangila.app.movie.persistance.outbox;

import com.github.mangila.app.shared.persistence.type.Status;
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Repository
public interface MovieOutboxJpaRepository extends BaseJpaRepository<MovieOutboxEntity, Integer> {

    <T> Optional<T> findById(UUID id, Class<T> type);

    @Modifying(
            clearAutomatically = true,
            flushAutomatically = true)
    @Query("""
            UPDATE movie_outbox o
            SET o.status = :status
            WHERE o.id IN :outboxIds
            """)
    void bulkChangeStatus(List<UUID> outboxIds, Status status);
}
