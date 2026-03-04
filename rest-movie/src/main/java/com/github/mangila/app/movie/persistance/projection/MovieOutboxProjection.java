package com.github.mangila.app.movie.persistance.projection;

import com.github.mangila.app.shared.persistence.type.Status;

import java.util.UUID;

public record MovieOutboxProjection(UUID id, UUID historyId, UUID aggregateId, Integer aggregateVersion, Status status) {
}
