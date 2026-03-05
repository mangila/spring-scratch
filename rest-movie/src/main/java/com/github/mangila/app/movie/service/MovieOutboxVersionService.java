package com.github.mangila.app.movie.service;

import com.github.mangila.app.movie.persistance.outbox.version.MovieOutboxVersionJdbcRepository;
import com.github.mangila.app.movie.persistance.outbox.version.MovieOutboxVersionJpaRepository;
import com.github.mangila.app.shared.chaos.Chaos;
import com.github.mangila.app.shared.persistence.base.projection.OutboxVersionProjection;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class MovieOutboxVersionService {

	private final MovieOutboxVersionJdbcRepository jdbc;

	private final MovieOutboxVersionJpaRepository jpa;

	public MovieOutboxVersionService(MovieOutboxVersionJdbcRepository movieOutboxVersionJdbcRepository,
			MovieOutboxVersionJpaRepository movieOutboxVersionJpaRepository) {
		this.jdbc = movieOutboxVersionJdbcRepository;
		this.jpa = movieOutboxVersionJpaRepository;
	}

	@Chaos
	@Transactional(propagation = Propagation.MANDATORY)
	public OutboxVersionProjection findVersionByIdWithXLock(@NotNull UUID id) {
		return jpa.findByIdWithXLock(id).orElseThrow();
	}

	@Chaos
	@Transactional(propagation = Propagation.MANDATORY)
	public void updateVersion(UUID uuid, Integer version) {
		jdbc.updateVersion(uuid, version);
	}

}
