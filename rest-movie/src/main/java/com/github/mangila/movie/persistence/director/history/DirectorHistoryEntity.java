package com.github.mangila.movie.persistence.director.history;

import com.github.mangila.movie.persistence.HistoryBaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity(name = "director_history")
@Table(name = "director_history")
public class DirectorHistoryEntity extends HistoryBaseEntity {

	public DirectorHistoryEntity() {
		// do nothing, for JPA
	}

}
