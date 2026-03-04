package com.github.mangila.app.shared.persistence.base.projection;

import java.util.UUID;

public record OutboxVersionProjection(UUID aggregateId, Integer currentVersion) {
}