package com.github.mangila.movie.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class MovieEntity {

    @Id
    private String id;

}
