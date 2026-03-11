package com.github.mangila.app.actor.persistence.outbox.destination;

import com.github.mangila.app.shared.persistence.base.OutboxDestinationBaseEntity;
import com.github.mangila.app.shared.persistence.type.Destination;
import com.github.mangila.app.shared.persistence.type.Status;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity(name = "actor_outbox_destination")
@Table(name = "actor_outbox_destination")
public class ActorOutboxDestinationEntity extends OutboxDestinationBaseEntity {

	public ActorOutboxDestinationEntity() {
		// do nothing, for JPA
	}

	public ActorOutboxDestinationEntity(UUID outboxId, Destination destination, Status status) {
		super(outboxId, destination, status);
	}

}
