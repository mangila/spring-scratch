package com.github.mangila.app.actor.persistence.outbox;

import com.github.mangila.app.shared.persistence.base.OutboxBaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity(name = "actor_outbox")
@Table(name = "actor_outbox")
public class ActorOutboxEntity extends OutboxBaseEntity {

	public ActorOutboxEntity() {
		// do nothing, for JPA
	}

}
