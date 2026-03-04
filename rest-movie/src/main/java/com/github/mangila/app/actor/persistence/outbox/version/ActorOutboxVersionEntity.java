package com.github.mangila.app.actor.persistence.outbox.version;

import com.github.mangila.app.shared.persistence.base.OutboxVersionBaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity(name = "actor_outbox_version")
@Table(name = "actor_outbox_version")
public class ActorOutboxVersionEntity extends OutboxVersionBaseEntity {

	public ActorOutboxVersionEntity() {
		// do nothing, for JPA
	}

}
