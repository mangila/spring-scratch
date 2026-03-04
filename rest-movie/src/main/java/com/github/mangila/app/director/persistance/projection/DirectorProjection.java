package com.github.mangila.app.director.persistance.projection;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

public record DirectorProjection(UUID id, String name, URI picture, String biography, LocalDate dateOfBirth) {
}
