package com.github.mangila.movie.web.api;

import com.github.mangila.movie.persistence.actor.projection.ActorProjection;
import com.github.mangila.movie.service.ActorService;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/actors")
public class ActorController {

	private final ActorService actorService;

	public ActorController(ActorService actorService) {
		this.actorService = actorService;
	}

	@GetMapping
	public List<ActorProjection> findAllProjections(Pageable pageable) {
		return actorService.findAllProjections(pageable);
	}

}
