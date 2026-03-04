package com.github.mangila.app.movie.persistance.outbox.destination;

import com.github.mangila.app.shared.persistence.base.OutboxDestinationBaseEntity;
import com.github.mangila.app.shared.persistence.type.Destination;
import com.github.mangila.app.shared.persistence.type.Status;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity(name = "movie_outbox_destination")
@Table(name = "movie_outbox_destination")
public class MovieOutboxDestinationEntity extends OutboxDestinationBaseEntity {

	public MovieOutboxDestinationEntity() {
		// do nothing, for JPA
	}

	public MovieOutboxDestinationEntity(UUID outboxId, Destination destination, Status status) {
		super(outboxId, destination, status);
	}

}
