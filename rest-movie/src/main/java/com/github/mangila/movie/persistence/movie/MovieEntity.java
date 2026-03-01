package com.github.mangila.movie.persistence.movie;

import com.github.mangila.movie.persistence.AuditBaseEntity;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity(name = "movie")
@Table(name = "movie")
public class MovieEntity extends AuditBaseEntity {

    @Id
    private UUID id;

    @Column(columnDefinition = "text", unique = true)
    private String title;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Set<String> genres = new HashSet<>();

    private BigDecimal budget;

    private LocalDate releaseDate;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Set<UUID> directors = new HashSet<>();

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Set<UUID> actors = new HashSet<>();

    public MovieEntity() {
        // do nothing, for JPA
    }

    public MovieEntity(UUID id, String title, Set<String> genres, BigDecimal budget, LocalDate releaseDate) {
        this.id = id;
        this.title = title;
        this.genres = genres;
        this.budget = budget;
        this.releaseDate = releaseDate;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String name) {
        this.title = name;
    }

    public Set<String> getGenres() {
        return genres;
    }

    public void setGenres(Set<String> genres) {
        this.genres = genres;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(LocalDate releaseDate) {
        this.releaseDate = releaseDate;
    }

    public Set<UUID> getDirectors() {
        return directors;
    }

    public void setDirectors(Set<UUID> directors) {
        this.directors = directors;
    }

    public Set<UUID> getActors() {
        return actors;
    }

    public void setActors(Set<UUID> actors) {
        this.actors = actors;
    }

}
