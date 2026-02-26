package com.github.mangila.movie.persistence.actor;

import java.net.URI;

public record ActorProjection(String id, String name, URI picture, String bio) {
}
