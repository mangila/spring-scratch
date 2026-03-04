package com.github.mangila.app.director.persistance.history;

import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DirectorHistoryJpaRepository extends BaseJpaRepository<DirectorHistoryEntity, Integer> {

}
