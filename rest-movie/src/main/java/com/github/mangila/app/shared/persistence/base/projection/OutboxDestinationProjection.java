package com.github.mangila.app.shared.persistence.base.projection;

import com.github.mangila.app.shared.persistence.type.Destination;
import com.github.mangila.app.shared.persistence.type.Status;

import java.util.UUID;

public record OutboxDestinationProjection(UUID id, UUID outboxId, Destination destination, Status status) {
}