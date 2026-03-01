package com.github.mangila.movie.persistence.movie.projection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record MovieProjection(UUID id, String name, Set<String> genres, BigDecimal budget, LocalDate releaseDate) {

}
