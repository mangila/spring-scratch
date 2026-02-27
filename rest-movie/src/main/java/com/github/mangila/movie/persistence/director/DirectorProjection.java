package com.github.mangila.movie.persistence.director;

import java.net.URI;

public record DirectorProjection(String id, String name, URI picture, String bio) {
}
