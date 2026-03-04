package com.github.mangila.app.movie.persistance.outbox;

import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MovieOutboxJpaRepository extends BaseJpaRepository<MovieOutboxEntity, Integer> {

	<T> Optional<T> findById(UUID id, Class<T> type);

}
