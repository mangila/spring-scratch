package com.github.mangila.app.movie.persistance.outbox;

import com.github.mangila.app.shared.persistence.base.projection.OutboxProjection;
import com.github.mangila.app.shared.persistence.type.Status;
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MovieOutboxJpaRepository extends BaseJpaRepository<MovieOutboxEntity, UUID> {

    <T> Optional<T> findById(UUID outboxId, Class<T> type);

    @Modifying
    @Query("""
            UPDATE movie_outbox o
            SET o.status = :to,
                o.updatedAt = CURRENT_TIMESTAMP
            WHERE o.id = :outboxId
            AND o.status = :from
            """)
    int changeStatus(UUID outboxId, Status from, Status to);

    List<OutboxProjection> findAllByStatus(@NotNull Status status, Limit limit);
}
