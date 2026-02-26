package com.github.mangila.movie.persistence.actor;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity(name = "actor")
@Table(name = "actor")
@EntityListeners(AuditingEntityListener.class)
public class ActorEntity {

    @Id
    private String id;

    @ElementCollection
    @CollectionTable(name = "actor_movie")
    private Set<String> movies = new HashSet<>();

    @Version
    private Integer version;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
