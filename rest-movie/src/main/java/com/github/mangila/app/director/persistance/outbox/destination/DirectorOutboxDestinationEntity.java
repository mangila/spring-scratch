package com.github.mangila.app.director.persistance.outbox.destination;

import com.github.mangila.app.shared.persistence.base.OutboxDestinationBaseEntity;
import com.github.mangila.app.shared.persistence.type.Destination;
import com.github.mangila.app.shared.persistence.type.Status;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity(name = "director_outbox_destination")
@Table(name = "director_outbox_destination")
public class DirectorOutboxDestinationEntity extends OutboxDestinationBaseEntity {

	public DirectorOutboxDestinationEntity() {
		// do nothing, for JPA
	}

	public DirectorOutboxDestinationEntity(UUID outboxId, Destination destination, Status status) {
		super(outboxId, destination, status);
	}

}
