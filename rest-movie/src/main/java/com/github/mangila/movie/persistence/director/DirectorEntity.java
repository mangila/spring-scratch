package com.github.mangila.movie.persistence.director;

import com.github.mangila.movie.persistence.AuditBaseEntity;
import com.github.mangila.movie.persistence.converter.UriConverter;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity(name = "director")
@Table(name = "director")
public class DirectorEntity extends AuditBaseEntity {

	@Id
	private UUID id;

	private String name;

	@Convert(converter = UriConverter.class)
	private URI picture;

	private String bio;

	@Type(JsonType.class)
	@Column(columnDefinition = "jsonb")
	private Set<UUID> movies = new HashSet<>();

	public DirectorEntity() {
		// do nothing, for JPA
	}

	public DirectorEntity(UUID id, String name, URI uri, String bio) {
		this.id = id;
		this.name = name;
		this.picture = uri;
		this.bio = bio;
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

	public String getBio() {
		return bio;
	}

	public void setBio(String bio) {
		this.bio = bio;
	}

	public Set<UUID> getMovies() {
		return movies;
	}

	public void setMovies(Set<UUID> movies) {
		this.movies = movies;
	}

}
