package com.github.mangila.app.movie.persistance.outbox;

import com.github.mangila.app.shared.persistence.type.Status;
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MovieOutboxJpaRepository extends BaseJpaRepository<MovieOutboxEntity, UUID> {

    @Modifying
    @Query("""
            UPDATE movie_outbox o
            SET o.status = :to,
                o.updatedAt = transaction_timestamp()
            WHERE o.id = :outboxId
            AND o.status = :from
            """)
    int changeStatus(UUID outboxId, Status from, Status to);
}
