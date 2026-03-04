package com.github.mangila.app.movie.persistance.projection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record MovieDetailsProjection(UUID id, String title, Set<String> genres, BigDecimal budget,
		LocalDate releaseDate) {

}
