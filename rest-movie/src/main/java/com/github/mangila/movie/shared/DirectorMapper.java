package com.github.mangila.movie.shared;

import com.github.mangila.movie.persistence.director.DirectorEntity;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
public class DirectorMapper {

	public DirectorEntity toEntity(CSVRecord record) {
		var id = record.get("id");
		var name = record.get("name");
		var picture = record.get("picture");
		var bio = record.get("bio");
		return new DirectorEntity(id, name, URI.create(picture), bio);
	}

}
