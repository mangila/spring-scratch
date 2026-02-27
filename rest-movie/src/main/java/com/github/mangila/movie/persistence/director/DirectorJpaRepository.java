package com.github.mangila.movie.persistence.director;

import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DirectorJpaRepository extends BaseJpaRepository<DirectorEntity, String> {

    <T> List<T> findAllBy(Class<T> type);

}
