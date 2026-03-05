package com.github.mangila.app.movie.persistance.outbox.destination;

import com.github.mangila.app.shared.persistence.type.Status;
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MovieOutboxDestinationJpaRepository extends BaseJpaRepository<MovieOutboxDestinationEntity, UUID> {

    @Modifying
    @Query("""
            UPDATE movie_outbox_destination d
            SET d.status = :status,
            	d.updatedAt = transaction_timestamp()
            WHERE d.id = :destinationId
            """)
    boolean updateStatus(@Param("destinationId") UUID destinationId, @Param("status") Status status);

}
