package com.github.mangila.app.actor.persistence.history;

import com.github.mangila.app.shared.persistence.base.HistoryBaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity(name = "actor_history")
@Table(name = "actor_history")
public class ActorHistoryEntity extends HistoryBaseEntity {

	public ActorHistoryEntity() {
		// do nothing, for JPA
	}

}
