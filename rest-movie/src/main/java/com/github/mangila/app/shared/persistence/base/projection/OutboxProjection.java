package com.github.mangila.app.shared.persistence.base.projection;

import com.github.mangila.app.shared.persistence.type.Status;

import java.util.UUID;

public record OutboxProjection(UUID id, UUID historyId, UUID aggregateId, Integer aggregateVersion,
                               Status status) {
}