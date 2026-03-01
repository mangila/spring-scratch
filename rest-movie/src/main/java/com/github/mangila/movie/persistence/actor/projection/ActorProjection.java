package com.github.mangila.movie.persistence.actor.projection;

import java.net.URI;
import java.util.UUID;

public record ActorProjection(UUID id, String name, URI picture, String bio) {
}
