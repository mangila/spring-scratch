package com.github.mangila.app.movie.persistance.outbox;

import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MovieOutboxJpaRepository extends BaseJpaRepository<MovieOutboxEntity, Integer> {

}
