package com.github.mangila.movie.persistence.movie;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MovieProjection(String id, String name, String genre, BigDecimal budget, LocalDate releaseDate) {
}

