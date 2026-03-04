package com.github.mangila.app.movie.persistance.projection;

import tools.jackson.databind.node.ObjectNode;

import java.util.UUID;

public record MovieHistoryProjection(UUID id,
                                     UUID aggregateId,
                                     Integer aggregateVersion,
                                     String operation,
                                     ObjectNode payload) {
}