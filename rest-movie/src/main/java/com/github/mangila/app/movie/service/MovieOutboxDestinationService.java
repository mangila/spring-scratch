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
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Validated
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
    public List<MovieOutboxDestinationEntity> createDestinations(@NotNull UUID id) {
        var destinations = movieProperties.getOutbox()
                .getDestinations()
                .stream()
                .map(destination -> new MovieOutboxDestinationEntity(id, destination, Status.PENDING))
                .toList();
        return jpa.persistAll(destinations);
    }

    @Chaos
    public boolean updateStatus(UUID destinationId, Status status) {
        int result = jpa.updateStatus(destinationId, status);
        return result > 0;
    }

    @Chaos
    public List<OutboxDestinationProjection> findAllByOutboxIdAndStatus(UUID outboxId, Status status) {
        return jpa.findAllByOutboxIdAndStatus(outboxId, status, OutboxDestinationProjection.class);
    }

    @Chaos
    public List<MovieOutboxDestinationEntity> findAllByStatus(Status status, @Positive int limit) {
        var sort = Sort.by(Sort.Direction.ASC, "updated_at", "created_at");
        return jpa.findAllByStatus(status, Limit.of(limit), sort);
    }

    @Chaos
    public void deleteAllById(ArrayList<UUID> destinationsIds) {
        jpa.deleteAllByIdInBatch(destinationsIds);
    }

    public List<MovieOutboxDestinationEntity> findAllByOutboxId(UUID outboxId) {
        return jpa.findAllByOutboxId(outboxId);
    }
}
