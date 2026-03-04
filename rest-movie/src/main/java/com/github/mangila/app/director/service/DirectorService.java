package com.github.mangila.app.director.service;

import com.github.mangila.app.director.persistance.DirectorJpaRepository;
import com.github.mangila.app.director.persistance.projection.DirectorProjection;
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
