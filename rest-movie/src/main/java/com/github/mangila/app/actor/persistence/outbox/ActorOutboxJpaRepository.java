package com.github.mangila.app.actor.persistence.outbox;

import com.github.mangila.app.actor.persistence.history.ActorHistoryEntity;
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActorOutboxJpaRepository extends BaseJpaRepository<ActorOutboxEntity, Integer> {

}
