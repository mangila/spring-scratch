package com.github.mangila.app.actor.persistence.projection;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

public record ActorProjection(UUID id, String name, URI picture, String biography, LocalDate dateOfBirth) {
}
