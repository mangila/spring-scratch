package com.github.mangila.app.director.persistance.outbox;

import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DirectorOutboxJpaRepository extends BaseJpaRepository<DirectorOutboxEntity, Integer> {

}
