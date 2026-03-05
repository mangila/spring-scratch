package com.github.mangila.app.movie.scheduler.outbox.relay.step;

import com.github.mangila.app.movie.service.MovieHistoryService;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class FetchPayloadStepHandler {

	private final MovieHistoryService movieHistoryService;

	public FetchPayloadStepHandler(MovieHistoryService movieHistoryService) {
		this.movieHistoryService = movieHistoryService;
	}

	public FetchPayloadStepResult handle(UUID historyId) {
		var payload = movieHistoryService.findPayloadById(historyId);
		return new FetchPayloadStepResult(payload);
	}

}
