package com.github.mangila.movie.web.api;

import com.github.mangila.movie.persistence.movie.projection.MovieProjection;
import com.github.mangila.movie.service.MovieService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/movies")
public class MovieController {

	private final MovieService movieService;

	public MovieController(MovieService movieService) {
		this.movieService = movieService;
	}

	@GetMapping
	public List<MovieProjection> findAllProjections() {
		return movieService.findAllProjections();
	}

}
