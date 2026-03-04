package com.github.mangila.app.movie.service;

import com.github.mangila.app.movie.persistance.outbox.destination.MovieOutboxDestinationEntity;
import com.github.mangila.app.movie.persistance.outbox.destination.MovieOutboxDestinationJdbcRepository;
import com.github.mangila.app.movie.persistance.outbox.destination.MovieOutboxDestinationJpaRepository;
import com.github.mangila.app.movie.properties.MovieProperties;
import com.github.mangila.app.shared.chaos.Chaos;
import com.github.mangila.app.shared.persistence.base.projection.OutboxDestinationProjection;
import com.github.mangila.app.shared.persistence.type.Status;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MovieOutboxDestinationService {

	private final MovieOutboxDestinationJdbcRepository jdbc;

	private final MovieOutboxDestinationJpaRepository jpa;

	private final MovieProperties movieProperties;

	public MovieOutboxDestinationService(MovieOutboxDestinationJdbcRepository movieOutboxDestinationJdbcRepository,
			MovieOutboxDestinationJpaRepository movieOutboxDestinationJpaRepository, MovieProperties movieProperties) {
		this.jdbc = movieOutboxDestinationJdbcRepository;
		this.jpa = movieOutboxDestinationJpaRepository;
		this.movieProperties = movieProperties;
	}

	@Chaos
	@Transactional(propagation = Propagation.MANDATORY)
	public List<OutboxDestinationProjection> claimDestinationPending(@Positive int limit) {
		return jdbc.claimDestinationPending(limit);
	}

	@Chaos
	public List<MovieOutboxDestinationEntity> createDestinations(@NotNull UUID id) {
		var destinations = movieProperties.getOutbox()
			.getDestinations()
			.stream()
			.map(destination -> new MovieOutboxDestinationEntity(id, destination, Status.PENDING))
			.toList();
		return jpa.persistAll(destinations);
	}

	@Chaos
	@Transactional(propagation = Propagation.MANDATORY)
	public void updateDestinationStatus(UUID destinationId, Status status) {
		jdbc.updateStatus(destinationId, status);
	}

}
