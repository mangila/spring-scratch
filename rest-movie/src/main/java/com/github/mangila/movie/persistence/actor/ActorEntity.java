package com.github.mangila.movie.persistence.actor;

import com.github.mangila.movie.persistence.AuditBaseEntity;
import com.github.mangila.movie.persistence.converter.UriConverter;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;

import java.net.URI;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity(name = "actor")
@Table(name = "actor")
public class ActorEntity extends AuditBaseEntity {

    @Id
    private UUID id;

    @Column(columnDefinition = "text", nullable = false)
    private String name;

    @Convert(converter = UriConverter.class)
    @Column(columnDefinition = "text", nullable = false)
    private URI picture;

    @Column(columnDefinition = "text", nullable = false)
    private String biography;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Set<UUID> movies = new HashSet<>();

    public ActorEntity() {
        // do nothing, for JPA
    }

    public ActorEntity(UUID id, String name, URI picture, String biography, LocalDate dateOfBirth) {
        this.id = id;
        this.name = name;
        this.picture = picture;
        this.biography = biography;
        this.dateOfBirth = dateOfBirth;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public URI getPicture() {
        return picture;
    }

    public void setPicture(URI picture) {
        this.picture = picture;
    }

    public String getBiography() {
        return biography;
    }

    public void setBiography(String bio) {
        this.biography = bio;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Set<UUID> getMovies() {
        return movies;
    }

    public void setMovies(Set<UUID> movies) {
        this.movies = movies;
    }

}
