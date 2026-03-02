package com.github.mangila.movie.persistence.outbox.version;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OutboxVersionJpaRepository extends JpaRepository<OutboxVersionEntity, UUID> {

}
