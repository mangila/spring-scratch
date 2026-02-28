package com.github.mangila.movie.persistence.actor.history;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "actor_history")
@Table(name = "actor_history")
public class ActorHistory {

    @Id
    private String id;

    private Integer version;

}
