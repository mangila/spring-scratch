package com.github.mangila.movie.persistence.movie.history;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MovieHistoryJpaRepository extends JpaRepository<MovieHistoryEntity, Integer> {
}
