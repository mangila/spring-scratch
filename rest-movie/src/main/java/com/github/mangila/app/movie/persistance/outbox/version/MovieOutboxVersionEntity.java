package com.github.mangila.app.movie.persistance.outbox.version;

import com.github.mangila.app.shared.persistence.base.OutboxVersionBaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity(name = "movie_outbox_version")
@Table(name = "movie_outbox_version")
public class MovieOutboxVersionEntity extends OutboxVersionBaseEntity {

    public MovieOutboxVersionEntity() {
        // do nothing, for JPA
    }

}
