package com.github.mangila.movie.service;

import com.github.mangila.movie.persistence.movie.MovieJdbcRepository;
import com.github.mangila.movie.persistence.movie.MovieJpaRepository;
import com.github.mangila.movie.persistence.movie.projection.MovieProjection;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MovieService {

    private final MovieJpaRepository movieJpaRepository;
    private final MovieJdbcRepository movieJdbcRepository;

    public MovieService(MovieJpaRepository movieJpaRepository,
                        MovieJdbcRepository movieJdbcRepository) {
        this.movieJpaRepository = movieJpaRepository;
        this.movieJdbcRepository = movieJdbcRepository;
    }

    public List<MovieProjection> findAllProjections() {
        return movieJpaRepository.findAllBy(MovieProjection.class);
    }
}
