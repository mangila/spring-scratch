package com.github.mangila.movie.service;

import com.github.mangila.movie.persistence.director.DirectorJpaRepository;
import com.github.mangila.movie.persistence.director.projection.DirectorProjection;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DirectorService {

	private final DirectorJpaRepository directorJpaRepository;

	public DirectorService(DirectorJpaRepository directorJpaRepository) {
		this.directorJpaRepository = directorJpaRepository;
	}

	public List<DirectorProjection> findAllProjections() {
		return directorJpaRepository.findAllBy(DirectorProjection.class);
	}

}
