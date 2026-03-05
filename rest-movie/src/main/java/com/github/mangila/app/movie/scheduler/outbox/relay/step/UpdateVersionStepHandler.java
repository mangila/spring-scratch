package com.github.mangila.app.movie.scheduler.outbox.relay.step;

import com.github.mangila.app.movie.service.MovieOutboxVersionService;
import com.github.mangila.app.shared.persistence.base.projection.OutboxProjection;
import org.springframework.stereotype.Component;

@Component
public class UpdateVersionStepHandler {

    private final MovieOutboxVersionService movieOutboxVersionService;

    public UpdateVersionStepHandler(MovieOutboxVersionService movieOutboxVersionService) {
        this.movieOutboxVersionService = movieOutboxVersionService;
    }

    public void handle(OutboxProjection outbox) {
        movieOutboxVersionService.updateVersion(outbox.aggregateId(), outbox.aggregateVersion() + 1);
    }
}
