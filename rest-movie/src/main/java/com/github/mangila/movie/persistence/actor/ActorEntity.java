package com.github.mangila.movie.persistence.actor;

import com.github.mangila.movie.persistence.converter.UriConverter;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.net.URI;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity(name = "actor")
@Table(name = "actor")
@EntityListeners(AuditingEntityListener.class)
public class ActorEntity {

	@Id
	private String id;

	private String name;

	@Convert(converter = UriConverter.class)
	private URI picture;

	private String bio;

	@Type(JsonType.class)
	@Column(columnDefinition = "jsonb")
	private Set<String> movies = new HashSet<>();

	@Version
	private Integer version;

	@CreatedDate
	@Column(updatable = false)
	private Instant createdAt;

	@LastModifiedDate
	private Instant updatedAt;

	public ActorEntity() {
		// do nothing, for JPA
	}

	public ActorEntity(String id, String name, URI picture, String bio) {
		this.id = id;
		this.name = name;
		this.picture = picture;
		this.bio = bio;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
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

	public Set<String> getMovies() {
		return movies;
	}

	public void setMovies(Set<String> movies) {
		this.movies = movies;
	}

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}

}
