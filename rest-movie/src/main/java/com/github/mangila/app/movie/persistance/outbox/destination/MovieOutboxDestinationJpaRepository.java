package com.github.mangila.app.movie.persistance.outbox.destination;

import com.github.mangila.app.shared.persistence.base.projection.OutboxDestinationProjection;
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MovieOutboxDestinationJpaRepository extends BaseJpaRepository<MovieOutboxDestinationEntity, Integer> {

    List<OutboxDestinationProjection> findAllByOutboxId(UUID outboxId);

}
