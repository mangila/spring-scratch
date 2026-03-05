package com.github.mangila.app.movie.scheduler.outbox.relay.step;

import com.github.mangila.app.movie.service.MovieOutboxDestinationService;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CreateDestinationStepHandler {

	private final MovieOutboxDestinationService destinationService;

	public CreateDestinationStepHandler(MovieOutboxDestinationService destinationService) {
		this.destinationService = destinationService;
	}

	public CreateDestinationStepResult handle(UUID outboxId) {
		var destinationEntities = destinationService.createDestinations(outboxId);
		return new CreateDestinationStepResult(destinationEntities);
	}

}
