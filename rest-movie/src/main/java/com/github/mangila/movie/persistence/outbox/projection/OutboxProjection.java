package com.github.mangila.movie.persistence.outbox.projection;

import com.github.mangila.movie.persistence.outbox.type.Status;

import java.util.UUID;

public record OutboxProjection(UUID id, UUID historyId, UUID aggregateId, Integer version, Status status) {
}
