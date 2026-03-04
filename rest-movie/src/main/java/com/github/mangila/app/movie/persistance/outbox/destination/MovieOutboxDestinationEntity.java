package com.github.mangila.app.movie.persistance.outbox.destination;

import com.github.mangila.app.shared.persistence.base.OutboxDestinationBaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity(name = "movie_outbox_destination")
@Table(name = "movie_outbox_destination")
public class MovieOutboxDestinationEntity extends OutboxDestinationBaseEntity {

    public MovieOutboxDestinationEntity() {
        // do nothing, for JPA
    }

}
