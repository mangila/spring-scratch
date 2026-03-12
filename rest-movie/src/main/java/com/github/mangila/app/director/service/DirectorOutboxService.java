package com.github.mangila.app.director.service;

import com.github.mangila.app.director.persistance.outbox.DirectorOutboxJdbcRepository;
import com.github.mangila.app.director.persistance.outbox.DirectorOutboxJpaRepository;
import com.github.mangila.app.shared.chaos.Chaos;
import com.github.mangila.app.shared.persistence.base.projection.OutboxProjection;
import com.github.mangila.app.shared.persistence.type.Status;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.UUID;

@Service
@Validated
public class DirectorOutboxService {

	private final DirectorOutboxJpaRepository jpa;

	private final DirectorOutboxJdbcRepository jdbc;

	public DirectorOutboxService(DirectorOutboxJpaRepository directorOutboxJpaRepository,
			DirectorOutboxJdbcRepository directorOutboxJdbcRepository) {
		this.jpa = directorOutboxJpaRepository;
		this.jdbc = directorOutboxJdbcRepository;
	}

	@Chaos
	@Transactional(propagation = Propagation.MANDATORY)
	public List<UUID> claimBatch(Status from, Status to, @Positive int limit) {
		return jdbc.claimBatch(from, to, limit);
	}

	@Chaos
	public List<OutboxProjection> findAllByStatus(@NotNull Status status, @Positive int limit) {
		return jpa.findAllByStatus(status, Limit.of(limit));
	}

	@Chaos
	public boolean changeStatus(UUID outboxId, Status from, Status to) {
		var result = jpa.changeStatus(outboxId, from, to);
		return result > 0;
	}

	@Chaos
	public void deleteAllById(List<UUID> list) {
		jpa.deleteAllByIdInBatch(list);
	}

	@Chaos
	public OutboxProjection findById(UUID outboxId) {
		return jpa.findById(outboxId, OutboxProjection.class).orElseThrow();
	}

}
