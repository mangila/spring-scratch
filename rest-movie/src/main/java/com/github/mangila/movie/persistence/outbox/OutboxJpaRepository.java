package com.github.mangila.movie.persistence.outbox;

import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OutboxJpaRepository extends BaseJpaRepository<OutboxEntity, UUID> {

}
