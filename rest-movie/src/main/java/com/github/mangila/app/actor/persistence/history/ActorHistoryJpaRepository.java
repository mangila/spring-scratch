package com.github.mangila.app.actor.persistence.history;

import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActorHistoryJpaRepository extends BaseJpaRepository<ActorHistoryEntity, Integer> {

}
