package com.github.mangila.movie.persistence.director.history;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DirectorHistoryJpaRepository extends JpaRepository<DirectorHistoryEntity, Integer> {
}
