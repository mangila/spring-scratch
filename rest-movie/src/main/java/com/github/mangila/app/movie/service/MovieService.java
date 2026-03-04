package com.github.mangila.app.movie.service;

import com.github.mangila.app.movie.persistance.MovieJdbcRepository;
import com.github.mangila.app.movie.persistance.MovieJpaRepository;
import com.github.mangila.app.movie.persistance.projection.MovieDetailsProjection;
import com.github.mangila.app.movie.persistance.projection.MovieProjection;
import com.github.mangila.app.movie.shared.MovieMapper;
import com.github.mangila.app.shared.chaos.Chaos;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MovieService {

	private final MovieJpaRepository movieJpaRepository;

	private final MovieMapper movieMapper;

	private final MovieJdbcRepository movieJdbcRepository;

	private final MovieHistoryService movieHistoryService;

	private final MovieOutboxService movieOutboxService;

	public MovieService(MovieJpaRepository movieJpaRepository, MovieMapper movieMapper,
			MovieJdbcRepository movieJdbcRepository, MovieHistoryService movieHistoryService,
			MovieOutboxService movieOutboxService) {
		this.movieJpaRepository = movieJpaRepository;
		this.movieMapper = movieMapper;
		this.movieJdbcRepository = movieJdbcRepository;
		this.movieHistoryService = movieHistoryService;
		this.movieOutboxService = movieOutboxService;
	}

	@Chaos
	public List<MovieDetailsProjection> findAllProjections() {
		return movieJpaRepository.findAllBy(MovieDetailsProjection.class);
	}

	@Chaos
	public void persistAll(List<MovieProjection> movieProjections) {
		var entities = movieMapper.toEntities(movieProjections);
		movieJpaRepository.persistAll(entities);
	}

}
