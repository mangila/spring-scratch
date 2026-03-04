package com.github.mangila.app.movie.persistance.history;

import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MovieHistoryJpaRepository extends BaseJpaRepository<MovieHistoryEntity, Integer> {

	<T> Optional<T> findById(UUID id, Class<T> type);

}
