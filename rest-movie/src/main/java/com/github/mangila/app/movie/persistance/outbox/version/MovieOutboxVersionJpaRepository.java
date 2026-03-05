package com.github.mangila.app.movie.persistance.outbox.version;

import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MovieOutboxVersionJpaRepository extends BaseJpaRepository<MovieOutboxVersionEntity, UUID> {

}
