package com.github.mangila.movie.shared;

import com.github.mangila.movie.persistence.movie.MovieEntity;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Component
public class MovieMapper {

	public MovieEntity toEntity(CSVRecord record) {
		var id = record.get("id");
		var name = record.get("name");
		var genre = record.get("genre");
		var budget = record.get("budget");
		var releaseDate = record.get("release_date");
		return new MovieEntity(UUID.fromString(id), name, genre, new BigDecimal(budget), LocalDate.parse(releaseDate));
	}

}
