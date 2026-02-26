package com.github.mangila.movie.persistence.movie;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity(name = "movie")
@Table(name = "movie")
@EntityListeners(AuditingEntityListener.class)
public class MovieEntity {

    @Id
    private String id;

    @ElementCollection
    @CollectionTable(name = "movie_director")
    private Set<String> directors = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "movie_actor")
    private Set<String> actors = new HashSet<>();

    @Version
    private Integer version;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
