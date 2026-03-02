package com.github.mangila.movie.persistence.director.history;

import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DirectorHistoryJpaRepository extends BaseJpaRepository<DirectorHistoryEntity, Integer> {

}
