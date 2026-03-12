package com.github.mangila.app.director.service;

import com.github.mangila.app.director.persistance.outbox.destination.DirectorOutboxDestinationEntity;
import com.github.mangila.app.director.persistance.outbox.destination.DirectorOutboxDestinationJdbcRepository;
import com.github.mangila.app.director.persistance.outbox.destination.DirectorOutboxDestinationJpaRepository;
import com.github.mangila.app.director.properties.DirectorProperties;
import com.github.mangila.app.shared.chaos.Chaos;
import com.github.mangila.app.shared.persistence.base.projection.OutboxDestinationProjection;
import com.github.mangila.app.shared.persistence.type.Status;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Validated
public class DirectorOutboxDestinationService {

	private final DirectorOutboxDestinationJdbcRepository jdbc;

	private final DirectorOutboxDestinationJpaRepository jpa;

	private final DirectorProperties directorProperties;

	public DirectorOutboxDestinationService(
			DirectorOutboxDestinationJdbcRepository directorOutboxDestinationJdbcRepository,
			DirectorOutboxDestinationJpaRepository directorOutboxDestinationJpaRepository,
			DirectorProperties directorProperties) {
		this.jdbc = directorOutboxDestinationJdbcRepository;
		this.jpa = directorOutboxDestinationJpaRepository;
		this.directorProperties = directorProperties;
	}

	@Chaos
	@Transactional(propagation = Propagation.MANDATORY)
	public List<DirectorOutboxDestinationEntity> createDestinations(@NotNull UUID id) {
		var destinations = directorProperties.getOutbox()
			.getDestinations()
			.stream()
			.map(destination -> new DirectorOutboxDestinationEntity(id, destination, Status.PENDING))
			.toList();
		return jpa.persistAll(destinations);
	}

	@Chaos
	public boolean updateStatus(UUID destinationId, Status from, Status to) {
		int result = jpa.changeStatus(destinationId, from, to);
		return result > 0;
	}

	@Chaos
	public List<OutboxDestinationProjection> findAllByOutboxIdAndStatus(UUID outboxId, Status status) {
		return jpa.findAllByOutboxIdAndStatus(outboxId, status, OutboxDestinationProjection.class);
	}

	@Chaos
	public List<DirectorOutboxDestinationEntity> findAllByStatus(Status status, @Positive int limit) {
		var sort = Sort.by(Sort.Direction.ASC, "updated_at", "created_at");
		return jpa.findAllByStatus(status, Limit.of(limit), sort);
	}

	@Chaos
	public void deleteAllById(ArrayList<UUID> destinationsIds) {
		jpa.deleteAllByIdInBatch(destinationsIds);
	}

	@Chaos
	public List<OutboxDestinationProjection> findAllByOutboxId(UUID outboxId) {
		return jpa.findAllByOutboxId(outboxId, OutboxDestinationProjection.class);
	}

	@Chaos
	public List<OutboxDestinationProjection> claimBatch(UUID outboxId, Status from, Status to) {
		return jdbc.claimBatch(outboxId, from, to);
	}

}
