package com.github.mangila.movie.persistence.actor.history;

import com.github.mangila.movie.persistence.HistoryBaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity(name = "actor_history")
@Table(name = "actor_history")
public class ActorHistoryEntity extends HistoryBaseEntity {

	public ActorHistoryEntity() {
		// do nothing, for JPA
	}

}
