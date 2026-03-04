package com.github.mangila.app.movie.service;

import com.github.mangila.app.movie.persistance.outbox.MovieOutboxJdbcRepository;
import com.github.mangila.app.movie.persistance.outbox.MovieOutboxJpaRepository;
import com.github.mangila.app.movie.persistance.outbox.destination.MovieOutboxDestinationJdbcRepository;
import com.github.mangila.app.movie.persistance.outbox.destination.MovieOutboxDestinationJpaRepository;
import com.github.mangila.app.movie.persistance.outbox.version.MovieOutboxVersionJdbcRepository;
import com.github.mangila.app.movie.persistance.outbox.version.MovieOutboxVersionJpaRepository;
import com.github.mangila.app.movie.persistance.projection.MovieOutboxProjection;
import com.github.mangila.app.movie.persistance.projection.MovieOutboxVersionProjection;
import com.github.mangila.app.shared.chaos.Chaos;
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

    private final MovieOutboxJpaRepository movieOutboxJpaRepository;
    private final MovieOutboxJdbcRepository movieOutboxJdbcRepository;
    private final MovieOutboxVersionJpaRepository movieOutboxVersionJpaRepository;
    private final MovieOutboxVersionJdbcRepository movieOutboxVersionJdbcRepository;
    private final MovieOutboxDestinationJpaRepository movieOutboxDestinationJpaRepository;
    private final MovieOutboxDestinationJdbcRepository movieOutboxDestinationJdbcRepository;

    public MovieOutboxService(MovieOutboxJpaRepository movieOutboxJpaRepository,
                              MovieOutboxJdbcRepository movieOutboxJdbcRepository,
                              MovieOutboxVersionJpaRepository movieOutboxVersionJpaRepository,
                              MovieOutboxVersionJdbcRepository movieOutboxVersionJdbcRepository,
                              MovieOutboxDestinationJpaRepository movieOutboxDestinationJpaRepository,
                              MovieOutboxDestinationJdbcRepository movieOutboxDestinationJdbcRepository) {
        this.movieOutboxJpaRepository = movieOutboxJpaRepository;
        this.movieOutboxJdbcRepository = movieOutboxJdbcRepository;
        this.movieOutboxVersionJpaRepository = movieOutboxVersionJpaRepository;
        this.movieOutboxVersionJdbcRepository = movieOutboxVersionJdbcRepository;
        this.movieOutboxDestinationJpaRepository = movieOutboxDestinationJpaRepository;
        this.movieOutboxDestinationJdbcRepository = movieOutboxDestinationJdbcRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public List<MovieOutboxProjection> claimPending(@Positive int limit) {
        return movieOutboxJdbcRepository.claimPending(limit);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public MovieOutboxVersionProjection findVersionByIdWithXLock(UUID id) {
        return movieOutboxVersionJpaRepository.findByIdWithXLock(id)
                .orElseThrow();
    }

    @Chaos(probability = 1, message = "CHAOS AAAAAHHHH!!")
    @Transactional(propagation = Propagation.MANDATORY)
    public void createDestinations(UUID id) {

    }
}
