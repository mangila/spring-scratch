package com.github.mangila.movie.persistence.actor;

import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActorJpaRepository extends BaseJpaRepository<ActorEntity, String> {
}
