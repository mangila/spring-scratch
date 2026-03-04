package com.github.mangila.app.movie.persistance.projection;

import java.util.UUID;

public record MovieOutboxVersionProjection(UUID aggregateId, Integer currentVersion) {
}