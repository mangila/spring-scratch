package com.github.mangila.movie.persistence.actor.history;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActorHistoryJpaRepository extends JpaRepository<ActorHistoryEntity, Integer> {
}
