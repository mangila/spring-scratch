package com.github.mangila.movie.persistence.movie;

import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MovieJpaRepository extends BaseJpaRepository<MovieEntity, String> {

}
