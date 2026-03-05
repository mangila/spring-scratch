package com.github.mangila.app.shared.persistence.base.projection;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.UUID;

public record HistoryProjection(UUID id, UUID aggregateId, Integer aggregateVersion, String operation,
                                JsonNode payload) {
}