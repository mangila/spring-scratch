package com.github.mangila.app.movie.persistance.outbox;

import com.github.mangila.app.shared.persistence.base.OutboxBaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity(name = "movie_outbox")
@Table(name = "movie_outbox")
public class MovieOutboxEntity extends OutboxBaseEntity {

    public MovieOutboxEntity() {
        // do nothing, for JPA
    }

}
