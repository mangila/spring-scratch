package com.github.mangila.movie.shared;

import com.github.mangila.movie.persistence.actor.ActorEntity;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
public class ActorMapper {

    public ActorEntity toEntity(CSVRecord record) {
        var id = record.get("id");
        var name = record.get("name");
        var picture = record.get("picture");
        var bio = record.get("bio");
        return new ActorEntity(id, name, URI.create(picture), bio);
    }
}
