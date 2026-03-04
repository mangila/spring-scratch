package com.github.mangila.app.actor.persistence.outbox.version;

import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActorOutboxVersionJpaRepository extends BaseJpaRepository<ActorOutboxVersionEntity, Integer> {

}
