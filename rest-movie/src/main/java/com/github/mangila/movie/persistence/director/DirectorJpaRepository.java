package com.github.mangila.movie.persistence.director;

import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DirectorJpaRepository extends BaseJpaRepository<DirectorEntity, String> {

}
