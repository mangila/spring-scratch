package com.github.mangila.app.movie.shared;

import com.github.mangila.app.movie.persistance.projection.MovieProjection;
import com.github.mangila.app.movie.persistance.MovieEntity;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class MovieMapper {

    private static final Pattern GENRES_SPLIT_PATTERN = Pattern.compile("\\|");

    public MovieProjection toProjection(CSVRecord record) {
        var id = record.get("id");
        var title = record.get("title");
        var genres = GENRES_SPLIT_PATTERN.splitAsStream(record.get("genres")).collect(Collectors.toSet());
        var budget = record.get("budget");
        var releaseDate = record.get("release_date");
        return new MovieProjection(
                UUID.fromString(id),
                title,
                genres,
                new BigDecimal(budget),
                LocalDate.parse(releaseDate),
                new HashSet<>(),
                new HashSet<>()
        );
    }

    public MovieEntity toEntity(MovieProjection movieProjection) {
        return new MovieEntity(
                movieProjection.id(),
                movieProjection.title(),
                movieProjection.genres(),
                movieProjection.budget(),
                movieProjection.releaseDate(),
                movieProjection.directors(),
                movieProjection.actors()
        );
    }

    public List<MovieEntity> toEntities(List<MovieProjection> movieProjections) {
        return movieProjections.stream()
                .map(this::toEntity)
                .toList();
    }
}
