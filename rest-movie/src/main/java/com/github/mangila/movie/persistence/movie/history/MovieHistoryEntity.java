package com.github.mangila.movie.persistence.movie.history;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Type;
import tools.jackson.databind.node.ObjectNode;

import java.time.Instant;

@Entity(name = "movie_history")
@Table(name = "movie_history")
@Immutable
public class MovieHistoryEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	private String aggregateId;

	private Integer version;

	private String operation;

	@Type(JsonType.class)
	@Column(columnDefinition = "jsonb")
	private ObjectNode payload;

	private Instant createdAt;

	public MovieHistoryEntity() {
		// do nothing, for JPA
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getAggregateId() {
		return aggregateId;
	}

	public void setAggregateId(String id) {
		this.aggregateId = id;
	}

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	public String getOperation() {
		return operation;
	}

	public void setOperation(String operation) {
		this.operation = operation;
	}

	public ObjectNode getPayload() {
		return payload;
	}

	public void setPayload(ObjectNode payload) {
		this.payload = payload;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

}
