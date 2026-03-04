package com.github.mangila.app.shared.persistence.base.projection;

import tools.jackson.databind.node.ObjectNode;

import java.util.UUID;

public record HistoryProjection(UUID id, UUID aggregateId, Integer aggregateVersion, String operation,
		ObjectNode payload) {
}