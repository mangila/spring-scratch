package com.github.mangila.app.movie.service;

import com.github.mangila.app.movie.persistance.outbox.MovieOutboxJdbcRepository;
import com.github.mangila.app.movie.persistance.outbox.MovieOutboxJpaRepository;
import com.github.mangila.app.shared.chaos.Chaos;
import com.github.mangila.app.shared.persistence.base.projection.OutboxProjection;
import com.github.mangila.app.shared.persistence.type.Status;
import jakarta.validation.constraints.Positive;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@Validated
public class MovieOutboxService {

	private final MovieOutboxJpaRepository jpa;

	private final MovieOutboxJdbcRepository jdbc;

	public MovieOutboxService(MovieOutboxJpaRepository movieOutboxJpaRepository,
			MovieOutboxJdbcRepository movieOutboxJdbcRepository) {
		this.jpa = movieOutboxJpaRepository;
		this.jdbc = movieOutboxJdbcRepository;
	}

	@Chaos
	@Transactional(propagation = Propagation.MANDATORY)
	public List<OutboxProjection> claimOutboxPending(@Positive int limit) {
		return jdbc.claimOutboxPending(limit);
	}

	@Chaos
	public OutboxProjection findOutboxById(UUID id) {
		return jpa.findById(id, OutboxProjection.class).orElseThrow();
	}

	@Chaos
	@Transactional(propagation = Propagation.MANDATORY)
	public Stream<UUID> streamOutboxIds(@Positive int limit) {
		return jdbc.streamOutboxIdsReadOnly(limit);
	}

	public void bulkChangeStatus(List<UUID> outboxIds, Status status) {
		jpa.bulkChangeStatus(outboxIds, status);
	}

}
