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

	private final MovieJdbcRepository jdbc;

	private final MovieJpaRepository jpa;

	private final MovieMapper movieMapper;

	public MovieService(MovieJdbcRepository movieJdbcRepository, MovieJpaRepository movieJpaRepository,
			MovieMapper movieMapper) {
		this.jdbc = movieJdbcRepository;
		this.jpa = movieJpaRepository;
		this.movieMapper = movieMapper;
	}

	@Chaos
	public List<MovieDetailsProjection> findAllProjections() {
		return jpa.findAllBy(MovieDetailsProjection.class);
	}

	@Chaos
	public void persistAll(List<MovieProjection> movieProjections) {
		var entities = movieMapper.toEntities(movieProjections);
		jpa.persistAll(entities);
	}

}
