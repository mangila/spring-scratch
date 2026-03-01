package com.github.mangila.movie.persistence.actor;

import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActorJpaRepository extends BaseJpaRepository<ActorEntity, String> {

	<T> List<T> findAllBy(Pageable pageable, Class<T> type);

}
