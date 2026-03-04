package com.github.mangila.app.actor.service;

import com.github.mangila.app.actor.persistence.ActorJpaRepository;
import com.github.mangila.app.actor.persistence.projection.ActorProjection;
import com.github.mangila.app.actor.shared.ActorMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ActorService {

	private final ActorJpaRepository jpaRepository;

	private final ActorMapper actorMapper;

	public ActorService(ActorJpaRepository jpaRepository, ActorMapper actorMapper) {
		this.jpaRepository = jpaRepository;
		this.actorMapper = actorMapper;
	}

	public List<ActorProjection> findAllProjections(Pageable pageable) {
		return jpaRepository.findAllBy(pageable, ActorProjection.class);
	}

}
