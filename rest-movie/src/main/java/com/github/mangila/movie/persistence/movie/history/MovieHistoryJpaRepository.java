package com.github.mangila.movie.persistence.movie.history;

import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MovieHistoryJpaRepository extends BaseJpaRepository<MovieHistoryEntity, Integer> {

}
