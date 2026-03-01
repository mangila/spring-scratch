package com.github.mangila.movie.persistence.director.projection;

import java.net.URI;
import java.util.UUID;

public record DirectorProjection(UUID id, String name, URI picture, String bio) {
}
