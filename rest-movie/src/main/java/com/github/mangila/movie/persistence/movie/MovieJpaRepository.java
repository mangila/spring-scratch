package com.github.mangila.movie.persistence.movie;

import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieJpaRepository extends BaseJpaRepository<MovieEntity, String> {

	<T> List<T> findAllBy(Class<T> type);

}
