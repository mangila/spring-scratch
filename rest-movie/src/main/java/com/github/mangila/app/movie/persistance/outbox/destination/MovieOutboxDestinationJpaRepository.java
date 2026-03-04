package com.github.mangila.app.movie.persistance.outbox.destination;

import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MovieOutboxDestinationJpaRepository extends BaseJpaRepository<MovieOutboxDestinationEntity, Integer> {

}
