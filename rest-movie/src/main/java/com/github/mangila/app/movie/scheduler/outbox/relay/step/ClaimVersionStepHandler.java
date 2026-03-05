package com.github.mangila.app.movie.scheduler.outbox.relay.step;

import com.github.mangila.app.movie.service.MovieOutboxVersionService;
import com.github.mangila.app.shared.persistence.base.projection.OutboxProjection;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;

@Component
public class ClaimVersionStepHandler {

	private final TransactionTemplate transactionTemplate;

	private final MovieOutboxVersionService movieOutboxVersionService;

	public ClaimVersionStepHandler(TransactionTemplate transactionTemplate,
			MovieOutboxVersionService movieOutboxVersionService) {
		this.transactionTemplate = transactionTemplate;
		this.movieOutboxVersionService = movieOutboxVersionService;
	}

	public boolean handle(OutboxProjection outbox) {
		final var aggregateId = outbox.aggregateId();
		final var version = outbox.aggregateVersion();
		return transactionTemplate.execute(_ -> {
			final var versionLock = movieOutboxVersionService.findVersionByIdWithXLock(aggregateId);
			final var currentVersion = versionLock.currentVersion();
			if (shouldProcess(version, currentVersion)) {
				movieOutboxVersionService.updateVersion(aggregateId, Integer.MAX_VALUE);
				return true;
			}
			return false;
		});
	}

	private static boolean shouldProcess(Integer aggregateVersion, Integer currentVersion) {
		Objects.requireNonNull(aggregateVersion, "aggregateVersion");
		Objects.requireNonNull(currentVersion, "currentVersion");
		return Objects.equals(aggregateVersion, currentVersion);
	}

}
