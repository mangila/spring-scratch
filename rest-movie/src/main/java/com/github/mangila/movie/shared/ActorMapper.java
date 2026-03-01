package com.github.mangila.movie.shared;

import com.github.mangila.movie.persistence.actor.ActorEntity;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

@Component
public class ActorMapper {

    public ActorEntity toEntity(CSVRecord record) {
        var id = record.get("id");
        var name = record.get("name");
        var picture = record.get("picture");
        var biography = record.get("biography");
        var dateOfBirth = record.get("date_of_birth");
        return new ActorEntity(UUID.fromString(id), name, URI.create(picture), biography, LocalDate.parse(dateOfBirth));
    }

}
