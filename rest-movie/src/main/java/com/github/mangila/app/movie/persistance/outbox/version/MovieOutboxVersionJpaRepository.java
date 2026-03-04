package com.github.mangila.app.movie.persistance.outbox.version;

import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MovieOutboxVersionJpaRepository extends BaseJpaRepository<MovieOutboxVersionEntity, Integer> {

}
