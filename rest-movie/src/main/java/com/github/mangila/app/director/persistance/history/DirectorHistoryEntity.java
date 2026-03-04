package com.github.mangila.app.director.persistance.history;

import com.github.mangila.app.shared.persistence.base.HistoryBaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity(name = "director_history")
@Table(name = "director_history")
public class DirectorHistoryEntity extends HistoryBaseEntity {

	public DirectorHistoryEntity() {
		// do nothing, for JPA
	}

}
