package com.github.mangila.movie.persistence.director.history;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "director_history")
@Table(name = "director_history")
public class DirectorHistory {

    @Id
    private String id;

    private Integer version;
}
