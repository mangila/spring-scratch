package com.github.mangila.app.director.persistance.outbox.version;

import com.github.mangila.app.shared.persistence.base.OutboxVersionBaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity(name = "director_outbox_version")
@Table(name = "director_outbox_version")
public class DirectorOutboxVersionEntity extends OutboxVersionBaseEntity {

	public DirectorOutboxVersionEntity() {
		// do nothing, for JPA
	}

}
