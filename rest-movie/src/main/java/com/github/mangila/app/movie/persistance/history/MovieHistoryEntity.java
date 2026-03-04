package com.github.mangila.app.movie.persistance.history;

import com.github.mangila.app.shared.persistence.base.HistoryBaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

@Entity(name = "movie_history")
@Table(name = "movie_history")
@Immutable
public class MovieHistoryEntity extends HistoryBaseEntity {

	public MovieHistoryEntity() {
		// do nothing, for JPA
	}

}
