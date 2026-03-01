package com.github.mangila.movie.persistence.movie;

import com.github.mangila.movie.persistence.AuditBaseEntity;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity(name = "movie")
@Table(name = "movie")
public class MovieEntity extends AuditBaseEntity {

	@Id
	private UUID id;

	private String name;

	private String genre;

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

	public MovieEntity(UUID id, String name, String genre, BigDecimal budget, LocalDate releaseDate) {
		this.id = id;
		this.name = name;
		this.genre = genre;
		this.budget = budget;
		this.releaseDate = releaseDate;
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

	public String getGenre() {
		return genre;
	}

	public void setGenre(String genre) {
		this.genre = genre;
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
