package com.github.mangila.app.director.persistance.outbox.destination;

import com.github.mangila.app.shared.persistence.type.Status;
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DirectorOutboxDestinationJpaRepository
		extends BaseJpaRepository<DirectorOutboxDestinationEntity, UUID> {

	<T> List<T> findAllByOutboxIdAndStatus(UUID outboxId, Status status, Class<T> type);

	@Modifying
	@Query("""
			UPDATE director_outbox_destination d
			SET d.status = :to,
			    d.updatedAt = CURRENT_TIMESTAMP
			WHERE d.id = :outboxId
			AND d.status = :from
			""")
	int changeStatus(UUID outboxId, Status from, Status to);

	List<DirectorOutboxDestinationEntity> findAllByStatus(Status status, Limit limit, Sort sort);

	<T> List<T> findAllByOutboxId(UUID outboxId, Class<T> type);

}
