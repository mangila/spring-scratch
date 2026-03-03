package com.github.mangila.movie.persistence.outbox;

import com.github.mangila.movie.persistence.outbox.type.Status;
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OutboxJpaRepository extends BaseJpaRepository<OutboxEntity, UUID> {

    @Modifying(
            clearAutomatically = true,
            flushAutomatically = true
    )
    @Query("""
            UPDATE outbox o
            SET o.status = :to
            WHERE o.id = :id
            AND o.status = :from
            """)
    int updateStatus(@Param("id") UUID id,
                     @Param("from") Status from,
                     @Param("to") Status to);
}
