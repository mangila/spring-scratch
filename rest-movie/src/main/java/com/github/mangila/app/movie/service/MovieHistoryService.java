package com.github.mangila.app.movie.service;

import com.github.mangila.app.movie.persistance.history.MovieHistoryJpaRepository;
import com.github.mangila.app.shared.persistence.base.projection.HistoryPayloadProjection;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class MovieHistoryService {

    private final MovieHistoryJpaRepository movieHistoryJpaRepository;

    public MovieHistoryService(MovieHistoryJpaRepository movieHistoryJpaRepository) {
        this.movieHistoryJpaRepository = movieHistoryJpaRepository;
    }

    public HistoryPayloadProjection findPayloadById(UUID historyId) {
        return movieHistoryJpaRepository.findById(historyId, HistoryPayloadProjection.class)
                .orElseThrow();
    }
}
