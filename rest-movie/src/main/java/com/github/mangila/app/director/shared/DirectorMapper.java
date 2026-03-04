package com.github.mangila.app.director.shared;

import com.github.mangila.app.director.persistance.DirectorEntity;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

@Component
public class DirectorMapper {

	public DirectorEntity toEntity(CSVRecord record) {
		var id = record.get("id");
		var name = record.get("name");
		var picture = record.get("picture");
		var biography = record.get("biography");
		var dateOfBirth = record.get("date_of_birth");
		return new DirectorEntity(UUID.fromString(id), name, URI.create(picture), biography,
				LocalDate.parse(dateOfBirth));
	}

}
