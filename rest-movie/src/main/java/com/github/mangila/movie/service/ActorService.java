package com.github.mangila.movie.service;

import com.github.mangila.movie.persistence.actor.ActorJpaRepository;
import com.github.mangila.movie.persistence.actor.ActorProjection;
import com.github.mangila.movie.shared.ActorMapper;
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

    public List<ActorProjection> findAllProjections() {
        return jpaRepository.findAllBy(ActorProjection.class);
    }

}
