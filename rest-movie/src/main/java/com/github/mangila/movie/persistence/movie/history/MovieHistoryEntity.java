package com.github.mangila.movie.persistence.movie.history;

import com.github.mangila.movie.persistence.HistoryBaseEntity;
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
