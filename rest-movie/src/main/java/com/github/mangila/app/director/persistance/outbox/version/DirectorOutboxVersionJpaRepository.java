package com.github.mangila.app.director.persistance.outbox.version;

import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DirectorOutboxVersionJpaRepository extends BaseJpaRepository<DirectorOutboxVersionEntity, Integer> {

}
