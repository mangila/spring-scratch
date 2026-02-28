package com.github.mangila.movie.persistence.movie.history;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "movie_history")
@Table(name = "movie_history")
public class MovieHistory {

    @Id
    private String id;

    private Integer version;
}
