package com.github.mangila.movie.persistence.actor;

import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActorJpaRepository extends BaseJpaRepository<ActorEntity, String> {

	<T> List<T> findAllBy(Class<T> type);

}
