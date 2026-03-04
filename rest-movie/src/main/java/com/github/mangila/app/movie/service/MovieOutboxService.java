package com.github.mangila.app.movie.service;

import com.github.mangila.app.movie.persistance.outbox.MovieOutboxJdbcRepository;
import com.github.mangila.app.movie.persistance.outbox.MovieOutboxJpaRepository;
import com.github.mangila.app.movie.persistance.outbox.destination.MovieOutboxDestinationEntity;
import com.github.mangila.app.movie.persistance.outbox.destination.MovieOutboxDestinationJdbcRepository;
import com.github.mangila.app.movie.persistance.outbox.destination.MovieOutboxDestinationJpaRepository;
import com.github.mangila.app.movie.persistance.outbox.version.MovieOutboxVersionJdbcRepository;
import com.github.mangila.app.movie.persistance.outbox.version.MovieOutboxVersionJpaRepository;
import com.github.mangila.app.movie.properties.MovieProperties;
import com.github.mangila.app.shared.chaos.Chaos;
import com.github.mangila.app.shared.persistence.base.projection.OutboxDestinationProjection;
import com.github.mangila.app.shared.persistence.base.projection.OutboxProjection;
import com.github.mangila.app.shared.persistence.base.projection.OutboxVersionProjection;
import com.github.mangila.app.shared.persistence.type.Status;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.UUID;

@Service
@Validated
public class MovieOutboxService {

	private final MovieProperties movieProperties;

	private final MovieOutboxJpaRepository movieOutboxJpaRepository;

	private final MovieOutboxJdbcRepository movieOutboxJdbcRepository;

	private final MovieOutboxVersionJpaRepository movieOutboxVersionJpaRepository;

	private final MovieOutboxVersionJdbcRepository movieOutboxVersionJdbcRepository;

	private final MovieOutboxDestinationJpaRepository movieOutboxDestinationJpaRepository;

	private final MovieOutboxDestinationJdbcRepository movieOutboxDestinationJdbcRepository;

	public MovieOutboxService(MovieProperties movieProperties, MovieOutboxJpaRepository movieOutboxJpaRepository,
			MovieOutboxJdbcRepository movieOutboxJdbcRepository,
			MovieOutboxVersionJpaRepository movieOutboxVersionJpaRepository,
			MovieOutboxVersionJdbcRepository movieOutboxVersionJdbcRepository,
			MovieOutboxDestinationJpaRepository movieOutboxDestinationJpaRepository,
			MovieOutboxDestinationJdbcRepository movieOutboxDestinationJdbcRepository) {
		this.movieProperties = movieProperties;
		this.movieOutboxJpaRepository = movieOutboxJpaRepository;
		this.movieOutboxJdbcRepository = movieOutboxJdbcRepository;
		this.movieOutboxVersionJpaRepository = movieOutboxVersionJpaRepository;
		this.movieOutboxVersionJdbcRepository = movieOutboxVersionJdbcRepository;
		this.movieOutboxDestinationJpaRepository = movieOutboxDestinationJpaRepository;
		this.movieOutboxDestinationJdbcRepository = movieOutboxDestinationJdbcRepository;
	}

	@Chaos
	@Transactional(propagation = Propagation.MANDATORY)
	public List<OutboxProjection> claimOutboxPending(@Positive int limit) {
		return movieOutboxJdbcRepository.claimOutboxPending(limit);
	}

	@Chaos
	public OutboxProjection findOutboxById(UUID id) {
		return movieOutboxJpaRepository.findById(id, OutboxProjection.class).orElseThrow();
	}

	@Chaos
	@Transactional(propagation = Propagation.MANDATORY)
	public List<OutboxDestinationProjection> claimDestinationPending(@Positive int limit) {
		return movieOutboxDestinationJdbcRepository.claimDestinationPending(limit);
	}

	@Chaos
	@Transactional(propagation = Propagation.MANDATORY)
	public OutboxVersionProjection findVersionByIdWithXLock(@NotNull UUID id) {
		return movieOutboxVersionJpaRepository.findByIdWithXLock(id).orElseThrow();
	}

	@Chaos
	@Transactional(propagation = Propagation.MANDATORY)
	public List<MovieOutboxDestinationEntity> createDestinations(@NotNull UUID id) {
		var destinations = movieProperties.getOutbox()
			.getDestinations()
			.stream()
			.map(destination -> new MovieOutboxDestinationEntity(id, destination, Status.PENDING))
			.toList();
		return movieOutboxDestinationJpaRepository.persistAll(destinations);
	}

}
