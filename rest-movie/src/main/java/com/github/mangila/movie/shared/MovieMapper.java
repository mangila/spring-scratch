package com.github.mangila.movie.shared;

import com.github.mangila.movie.persistence.movie.MovieEntity;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class MovieMapper {

    public MovieEntity toEntity(CSVRecord record) {
        var id = record.get("id");
        var title = record.get("title");
        var genres = Pattern.compile("\\|")
                .splitAsStream(record.get("genres"))
                .collect(Collectors.toSet());
        var budget = record.get("budget");
        var releaseDate = record.get("release_date");
        return new MovieEntity(UUID.fromString(id), title, genres, new BigDecimal(budget), LocalDate.parse(releaseDate));
    }

}
